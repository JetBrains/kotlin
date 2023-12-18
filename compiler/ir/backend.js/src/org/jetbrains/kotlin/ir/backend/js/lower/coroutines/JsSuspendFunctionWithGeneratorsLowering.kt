/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

private val SUSPEND_FUNCTION_AS_GENERATOR by IrDeclarationOriginImpl

class JsSuspendFunctionWithGeneratorsLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val getContinuationSymbol = context.ir.symbols.getContinuation
    private val jsYieldFunctionSymbol = context.intrinsics.jsYieldFunctionSymbol
    private val suspendOrReturnFunctionSymbol = context.intrinsics.suspendOrReturnFunctionSymbol
    private val coroutineSuspendedGetterSymbol = context.coroutineSymbols.coroutineSuspendedGetter

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && declaration.isSuspend) {
            return transformSuspendFunction(declaration)
        }
        return null
    }

    private fun transformSuspendFunction(function: IrSimpleFunction): List<IrFunction>? {
        val originalReturnType = function.returnType.also { function.returnType = context.irBuiltIns.anyNType }
        val body = function.body ?: return null
        return when (val functionKind = getSuspendFunctionKind(context, function, body, includeSuspendLambda = false)) {
            is SuspendFunctionKind.NO_SUSPEND_CALLS -> null
            is SuspendFunctionKind.DELEGATING -> {
                removeReturnIfSuspendedCallAndSimplifyDelegatingCall(function, functionKind.delegatingCall)
                null
            }
            is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                generateGeneratorAndItsWrapper(function, body, originalReturnType)
            }
        }
    }

    private fun IrSimpleFunction.addJsGeneratorAnnotation() {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(
            context.intrinsics.jsGeneratorAnnotationSymbol.owner.primaryConstructor!!.symbol
        )
    }

    private fun generateGeneratorAndItsWrapper(
        function: IrSimpleFunction,
        functionBody: IrBody,
        originalReturnType: IrType
    ): List<IrFunction> {
        val generatorFunction = context.irFactory.createSimpleFunction(
            function.startOffset,
            function.endOffset,
            SUSPEND_FUNCTION_AS_GENERATOR,
            Name.special("<generator-${function.name.asString()}>"),
            DescriptorVisibilities.PRIVATE,
            function.isInline,
            function.isExpect,
            originalReturnType,
            function.modality,
            IrSimpleFunctionSymbolImpl(),
            function.isTailrec,
            function.isSuspend,
            function.isOperator,
            function.isInfix,
            function.isExternal,
        ).apply {
            copyParameterDeclarationsFrom(function)
            parent = function.parent
            annotations = function.annotations
            body = functionBody.apply {
                val valueSymbols = function.valueParameters.zip(valueParameters)
                    .plus(function.dispatchReceiverParameter to dispatchReceiverParameter)
                    .plus(function.extensionReceiverParameter to extensionReceiverParameter)
                    .mapNotNull { (old, new) -> new?.let { old?.symbol?.to(it.symbol) } }
                    .toMap<IrValueSymbol, IrValueSymbol>()
                transformChildrenVoid(object : ValueRemapper(valueSymbols) {
                    override fun visitCall(expression: IrCall): IrExpression {
                        val call = super.visitCall(expression)
                        return if (call !is IrCall || !call.symbol.owner.isSuspend) {
                            call
                        } else {
                            context.createIrBuilder(call.symbol).run {
                                irBlock(resultType = call.type) {
                                    val tmp = createTmpVariable(call, irType = context.irBuiltIns.anyNType)
                                    val coroutineSuspended = irCall(coroutineSuspendedGetterSymbol)
                                    val condition = irEqeqeq(irGet(tmp), coroutineSuspended)
                                    val yield = irCall(jsYieldFunctionSymbol).apply { putValueArgument(0, irGet(tmp)) }
                                    +irIfThen(context.irBuiltIns.unitType, condition, irSet(tmp, yield))
                                    +irImplicitCast(irGet(tmp), call.type)
                                }
                            }
                        }
                    }
                })
            }
            addJsGeneratorAnnotation()
        }

        function.body = context.createIrBuilder(function.symbol).irBlockBody {
            +irReturn(
                irCall(suspendOrReturnFunctionSymbol).also {
                    it.putValueArgument(0, irCall(generatorFunction.symbol).apply {
                        dispatchReceiver = function.dispatchReceiverParameter?.let(::irGet)
                        extensionReceiver = function.extensionReceiverParameter?.let(::irGet)
                        contextReceiversCount = function.contextReceiverParametersCount
                        function.valueParameters.forEachIndexed { i, v -> putValueArgument(i, irGet(v)) }
                    })
                    it.putValueArgument(1, irCall(getContinuationSymbol))
                }
            )
        }

        return listOf(generatorFunction, function)
    }

    private fun removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction: IrFunction, delegatingCall: IrCall) {
        val returnValue = runIf(delegatingCall.isReturnIfSuspendedCall(context)) {
            delegatingCall.getValueArgument(0)
        } ?: delegatingCall

        val body = irFunction.body as IrBlockBody

        context.createIrBuilder(
            irFunction.symbol,
            startOffset = body.endOffset.previousOffset,
            endOffset = body.endOffset.previousOffset
        ).run {
            val statements = body.statements
            val lastStatement = statements.last()
            assert(lastStatement == delegatingCall || lastStatement is IrReturn) { "Unexpected statement $lastStatement" }

            val tempVar = scope.createTemporaryVariable(returnValue, irType = context.irBuiltIns.anyType)
            statements[statements.lastIndex] = tempVar
            statements.add(irReturn(irGet(tempVar)))
        }
    }
}