/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

object ES6_CONSTRUCTOR_REPLACEMENT : IrDeclarationOriginImpl("ES6_CONSTRUCTOR_REPLACEMENT")

class ES6ConstructorUsageLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    private var IrConstructor.initFunction by context.mapping.constructorToInitFunction

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrSimpleFunction) return

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                val replacement = expression.symbol.owner.initFunction ?: return super.visitDelegatingConstructorCall(expression)
                return JsIrBuilder.buildCall(replacement.symbol).apply {
                    copyValueArgumentsFrom(expression, symbol.owner)
                    val currentExtensionReceiver = container.extensionReceiverParameter ?: return@apply
                    extensionReceiver = JsIrBuilder.buildGetValue(currentExtensionReceiver.symbol)
                }
            }
        })
    }
}

class ES6ConstructorLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    private var IrConstructor.initFunction by context.mapping.constructorToInitFunction
    private var IrConstructor.constructorFactory by context.mapping.secondaryConstructorToFactory

    private val delegatedExternalSuperCalls = mutableMapOf<IrConstructorSymbol, IrDelegatingConstructorCall?>()

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrConstructor || declaration.hasStrictSignature(context)) return null

        hackEnums(declaration)
        hackExceptions(context, declaration)

        val superCall = declaration.getSuperCall()
        val initMethod = declaration.generateInitMethod()
        val factoryFunction = declaration.generateCreateFunction(superCall, initMethod)


        return listOf(factoryFunction, initMethod)
    }

    private fun IrConstructor.generateInitMethod(): IrSimpleFunction {
        val constructor = this
        val parent = parentAsClass
        val constructorName = "${parent.name}_init"
        val functionName = "${constructorName}_\$Init\$"

        return context.irFactory.buildFun {
            name = Name.identifier(functionName)
            returnType = context.irBuiltIns.unitType
            visibility = DescriptorVisibilities.PRIVATE
            modality = Modality.FINAL
            isInline = constructor.isInline
            isExternal = constructor.isExternal
            origin = ES6_CONSTRUCTOR_REPLACEMENT
        }.also { factory ->
            factory.parent = parent
            factory.copyTypeParametersFrom(parent)
            factory.extensionReceiverParameter = parent.thisReceiver?.copyTo(factory)
            factory.valueParameters = valueParameters.map { it.copyTo(factory) }

            factory.body = body?.deepCopyWithSymbols(factory)?.apply {
                val remappingSchema = constructor.valueParameters.asSequence()
                    .zip(factory.valueParameters.asSequence())
                    .map { it.first.symbol to it.second.symbol }
                    .plus(parent.thisReceiver!!.symbol to factory.extensionReceiverParameter!!.symbol)
                    .toMap<IrValueSymbol, IrValueSymbol>()

                removeExternalSuperCallWithRemapping(remappingSchema)
            }
            factory.annotations = annotations
            factory.dispatchReceiverParameter = null

            constructor.initFunction = factory
        }
    }

    private fun IrConstructor.generateCreateFunction(superCall: IrDelegatingConstructorCall?, initMethod: IrSimpleFunction): IrFunction {
        val function = if (isPrimary) {
            this
        } else {
            val irClass = parentAsClass
            val type = irClass.defaultType
            val constructorName = "${irClass.name}_init"
            val functionName = "${constructorName}_\$Create\$"
            val constructor = this

            context.irFactory.buildFun {
                name = Name.identifier(functionName)
                returnType = type
                visibility = constructor.visibility
                modality = Modality.FINAL
                isInline = constructor.isInline
                isExternal = constructor.isExternal
            }.also { factory ->
                factory.parent = irClass
                factory.copyTypeParametersFrom(irClass)
                factory.valueParameters += valueParameters.map { it.copyTo(factory) }
                factory.annotations = annotations

                constructorFactory = factory
            }
        }

        function.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            val thisVariable = generateThisVariable(superCall).also { statements += it }
            statements += JsIrBuilder.buildCall(initMethod.symbol).apply {
                extensionReceiver = JsIrBuilder.buildGetValue(thisVariable.symbol)
                function.valueParameters.forEachIndexed { i, p -> putValueArgument(i, JsIrBuilder.buildGetValue(p.symbol)) }
            }
            statements += JsIrBuilder.buildReturn(function.symbol, JsIrBuilder.buildGetValue(thisVariable.symbol), function.returnType)
        }

        return function
    }

    private fun IrConstructor.generateThisVariable(superCall: IrDelegatingConstructorCall?): IrVariable {
        val currentClass = parentAsClass
        val superClass = currentClass.superClass
        val currentCtor = currentClass.constructorRef

        val initializer = if (superClass?.isEffectivelyExternal() == true) {
            val superCtor = superCall?.getExternallyDelegatedSuperCall()?.symbol?.owner

            JsIrBuilder.buildCall(context.intrinsics.jsCreateThisFromParentSymbol).apply {
                putValueArgument(0, currentCtor)
                putValueArgument(1, (superCtor?.parentAsClass ?: superClass).constructorRef)
                putValueArgument(2, irAnyArray(superCall?.valueArguments ?: emptyList()))
            }
        } else {
            JsIrBuilder.buildCall(context.intrinsics.jsCreateThisSymbol).apply {
                putValueArgument(0, currentCtor)
            }
        }

        return JsIrBuilder.buildVar(
            type = returnType,
            parent = this,
            name = Namer.SYNTHETIC_RECEIVER_NAME,
            initializer = initializer
        )
    }

    private val IrFunctionAccessExpression.valueArguments: List<IrExpression>
        get() = List(valueArgumentsCount) { getValueArgument(it) ?: context.getVoid() }

    private val IrClass.constructorRef: IrExpression
        get() = JsIrBuilder.buildCall(context.intrinsics.jsClass).apply {
            putTypeArgument(0, defaultType)
        }

    private fun irAnyArray(elements: List<IrExpression>): IrExpression {
        return JsIrBuilder.buildArray(
            elements,
            context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.anyNType),
            context.irBuiltIns.anyNType,
        )
    }

    private fun IrConstructor.getSuperCall(): IrDelegatingConstructorCall? {
        var result: IrDelegatingConstructorCall? = null
        (body as IrBlockBody).acceptChildren(object : IrElementVisitor<Unit, Any?> {
            override fun visitElement(element: IrElement, data: Any?) { }

            override fun visitBlock(expression: IrBlock, data: Any?) {
                expression.statements.forEach { it.accept(this, data) }
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Any?) {
                result = result ?: expression
            }
        }, null)
        return result
    }

    private fun IrDelegatingConstructorCall.getExternallyDelegatedSuperCall(): IrDelegatingConstructorCall? =
        if (symbol.owner.parentAsClass.isEffectivelyExternal()) this else delegatedExternalSuperCalls.getOrPut(symbol) {
            symbol.owner.getSuperCall()?.getExternallyDelegatedSuperCall()
        }

    private fun IrBody.removeExternalSuperCallWithRemapping(remappings: Map<IrValueSymbol, IrValueSymbol>) {
        transformChildrenVoid(object : ValueRemapper(remappings) {
            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                return if (expression.symbol.owner.isEffectivelyExternal()) {
                    JsIrBuilder.buildGetObjectValue(context.irBuiltIns.unitType, context.irBuiltIns.unitClass)
                } else {
                    super.visitDelegatingConstructorCall(expression)
                }
            }
        })
    }
}

private fun hackEnums(constructor: IrConstructor) {
    constructor.transformChildren(object : IrElementTransformerVoid() {
        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
            return (expression.argument as? IrDelegatingConstructorCall) ?: expression
        }
    }, null)
}

/**
 * Swap call synthetic primary ctor and call extendThrowable
 */
private fun hackExceptions(context: JsIrBackendContext, constructor: IrConstructor) {
    val setPropertiesSymbol = context.setPropertiesToThrowableInstanceSymbol

    val statements = (constructor.body as IrBlockBody).statements

    var callIndex = -1
    var superCallIndex = -1
    for (i in statements.indices) {
        val s = statements[i]

        if (s is IrCall && s.symbol === setPropertiesSymbol) {
            callIndex = i
        }
        if (s is IrDelegatingConstructorCall && s.symbol.owner.origin === PrimaryConstructorLowering.SYNTHETIC_PRIMARY_CONSTRUCTOR) {
            superCallIndex = i
        }
    }

    if (callIndex != -1 && superCallIndex != -1) {
        val tmp = statements[callIndex]
        statements[callIndex] = statements[superCallIndex]
        statements[superCallIndex] = tmp
    }
}

private fun IrConstructor.hasStrictSignature(context: JsIrBackendContext): Boolean {
    return with(parentAsClass) { isExternal || isExpect || context.inlineClassesUtils.isClassInlineLike(this) }
}
