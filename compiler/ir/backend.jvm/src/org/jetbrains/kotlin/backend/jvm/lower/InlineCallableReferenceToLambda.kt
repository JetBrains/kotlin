/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.copyValueParametersToStatic
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrExpression
import org.jetbrains.kotlin.codegen.AsmUtil.BOUND_REFERENCE_RECEIVER
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal val inlineCallableReferenceToLambdaPhase = makeIrFilePhase(
    ::InlineCallableReferenceToLambdaPhase,
    name = "InlineCallableReferenceToLambdaPhase",
    description = "Transform callable reference to inline lambda"
)

// This lowering transforms CR passed to inline function to lambda which would be inlined
//
//      inline fun foo(inlineParameter: (A) -> B): B {
//          return inlineParameter()
//      }
//
//      foo(::smth) -> foo { a -> smth(a) }
//
internal class InlineCallableReferenceToLambdaPhase(val context: JvmBackendContext) : FileLoweringPass {
    private val inlinableCR = mutableSetOf<IrCallableReference>()

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val callee = expression.symbol.owner
                if (callee.isInlineFunctionCall(context)) {
                    for (valueParameter in callee.valueParameters) {
                        if (valueParameter.isInlineParameter()) {
                            expression.getValueArgument(valueParameter.index)?.let { argument ->
                                if (argument is IrCallableReference && isInlineIrExpression(argument)) {
                                    inlinableCR.add(argument)
                                }
                            }
                        }
                    }
                }

                return super.visitFunctionAccess(expression)
            }


            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                if (expression !in inlinableCR) {
                    return super.visitPropertyReference(expression)
                }

                //Use getter if field is absent...
                val field =
                    expression.field?.owner ?: return functionReferenceToLambda(currentScope!!, expression, expression.getter!!.owner)

                //..else use field itself
                val irBuilder =
                    context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                val boundReceiver = expression.dispatchReceiver ?: expression.extensionReceiver
                return irBuilder.irBlock(expression, IrStatementOrigin.LAMBDA) {
                    lateinit var variableForBoundReceiver: IrVariable
                    if (boundReceiver != null) {
                        variableForBoundReceiver = createTmpVariable(boundReceiver, BOUND_REFERENCE_RECEIVER)
                    }

                    val newLambda = buildFun {
                        setSourceRange(expression)
                        origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                        name = Name.identifier("stub_for_inline")
                        visibility = Visibilities.LOCAL
                        returnType = field.type
                        isSuspend = false
                    }.apply {

                        val receiver =
                            when {
                                field.isStatic -> null
                                boundReceiver != null -> variableForBoundReceiver
                                else -> addValueParameter("receiver", field.parentAsClass.defaultType)
                            }

                        val lambdaBodyBuilder = this@InlineCallableReferenceToLambdaPhase.context.createIrBuilder(this.symbol)
                        body = lambdaBodyBuilder.irBlockBody(startOffset, endOffset) {
                            +irReturn(irGetField(if (receiver != null) irGet(receiver) else null, field))
                        }
                    }
                    +newLambda

                    +IrFunctionReferenceImpl(
                        expression.startOffset, expression.endOffset, field.type,
                        newLambda.symbol, newLambda.symbol.descriptor, 0,
                        IrStatementOrigin.LAMBDA
                    ).apply {
                        copyAttributes(expression)
                    }
                }
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                if (inlinableCR.contains(expression)) {
                    val referencedFunction = expression.symbol.owner
                    return functionReferenceToLambda(currentScope!!, expression, referencedFunction)
                }

                return super.visitFunctionReference(expression)
            }
        })
    }

    private fun functionReferenceToLambda(
        scope: ScopeWithIr,
        expression: IrCallableReference,
        referencedFunction: IrFunction
    ): IrExpression {
        val irBuilder =
            context.createIrBuilder(scope.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)

        val boundReceiver = expression.dispatchReceiver ?: expression.extensionReceiver
        return irBuilder.irBlock(expression, IrStatementOrigin.LAMBDA) {
            lateinit var variableForBoundReceiver: IrVariable
            if (boundReceiver != null) {
                variableForBoundReceiver = createTmpVariable(boundReceiver, BOUND_REFERENCE_RECEIVER)
            }

            val newLambda = buildFun {
                setSourceRange(expression)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                name = Name.identifier("stub_for_inline")
                visibility = Visibilities.LOCAL
                returnType = referencedFunction.returnType
                isSuspend = false
            }.apply {
                if (referencedFunction is IrConstructor) {
                    copyTypeParametersFrom(referencedFunction.parentAsClass)
                }
                copyTypeParametersFrom(referencedFunction)
                if (boundReceiver == null) {
                    copyValueParametersToStatic(referencedFunction, origin)
                } else {
                    for (oldValueParameter in referencedFunction.valueParameters) {
                        valueParameters.add(
                            oldValueParameter.copyTo(
                                this,
                                origin = origin,
                                index = oldValueParameter.index
                            )
                        )
                    }
                }
                val lambdaBodyBuilder = this@InlineCallableReferenceToLambdaPhase.context.createIrBuilder(this.symbol)
                body = lambdaBodyBuilder.irBlockBody(startOffset, endOffset) {
                    var shift = 0
                    val irCall =
                        if (expression is IrPropertyReference)
                            irGet(referencedFunction.returnType, null, referencedFunction.symbol)
                        else irCall(referencedFunction.symbol)

                    +irReturn(
                        irCall.also { call ->
                            for (it in this@apply.typeParameters) {
                                call.putTypeArgument(it.index, expression.getTypeArgument(it.index))
                            }

                            referencedFunction.dispatchReceiverParameter?.let {
                                call.dispatchReceiver =
                                    irGet(if (expression.dispatchReceiver != null) variableForBoundReceiver else valueParameters[shift++])
                            }
                            referencedFunction.extensionReceiverParameter?.let {
                                call.extensionReceiver =
                                    irGet(if (expression.extensionReceiver != null) variableForBoundReceiver else valueParameters[shift++])
                            }

                            for (it in referencedFunction.valueParameters.indices) {
                                call.putValueArgument(it, irGet(valueParameters[shift++]))
                            }
                        }
                    )
                }
            }
            +newLambda

            +IrFunctionReferenceImpl(
                expression.startOffset, expression.endOffset, referencedFunction.returnType,
                newLambda.symbol, newLambda.symbol.descriptor, referencedFunction.typeParameters.size,
                IrStatementOrigin.LAMBDA
            ).apply {
                copyAttributes(expression)
            }
        }
    }
}