/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private object ES6_CONSTRUCTOR_REPLACEMENT : IrDeclarationOriginImpl("ES6_CONSTRUCTOR_REPLACEMENT")
private object ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT: IrStatementOriginImpl("ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT")

val IrFunction.isSyntheticEs6Constructor: Boolean
    get() = origin == ES6_CONSTRUCTOR_REPLACEMENT

val IrFunctionAccessExpression.isSyntheticDelegatingReplacement: Boolean
    get() = origin == ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT

class ES6ConstructorLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    private var IrConstructor.constructorFactory by context.mapping.secondaryConstructorToFactory

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrConstructor || declaration.hasStrictSignature(context)) return null

        hackEnums(declaration)
        hackExceptions(context, declaration)

        return listOf(declaration.generateCreateFunction())
    }

    private fun IrConstructor.generateCreateFunction(): IrFunction {
        val constructor = this
        val irClass = parentAsClass
        val type = irClass.defaultType
        val constructorName = "new_${irClass.constructorPostfix}"

        return context.irFactory.buildFun {
            name = Name.identifier(constructorName)
            returnType = type
            visibility = constructor.visibility
            modality = Modality.FINAL
            isInline = constructor.isInline
            isExternal = constructor.isExternal
            origin = ES6_CONSTRUCTOR_REPLACEMENT
        }.also { factory ->
            factory.parent = irClass
            factory.copyTypeParametersFrom(irClass)
            factory.valueParameters += valueParameters.map { it.copyTo(factory) }
            factory.annotations = annotations
            factory.dispatchReceiverParameter = irClass.thisReceiver?.copyTo(factory)

            val boxParameter = factory.generateBoxParameter(irClass).also { factory.valueParameters += it }
            val selfParameter = runIf(irClass.superClass?.isExternal != true) {
                JsIrBuilder.buildCall(context.intrinsics.jsCreateThisSymbol)
                    .apply { putValueArgument(0, irClass.constructorRef) }
                    .let { generateThisVariable(irClass, it) }
            }

            factory.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                val bodyCopy = constructor.body?.deepCopyWithSymbols(factory) ?: return@createBlockBody
                val (self, isBoxUsed) = bodyCopy.replaceSuperCallsAndThisUsages(
                    irClass,
                    selfParameter?.symbol ?: boxParameter.symbol,
                    factory
                )

                if (isBoxUsed) {
                    boxParameter.defaultValue = createJsObjectLiteral()
                }

                statements.addAll(bodyCopy.statements)
                statements.add(JsIrBuilder.buildReturn(factory.symbol, JsIrBuilder.buildGetValue(self), irClass.defaultType))
            }

            constructorFactory = factory
        }
    }

    private fun IrFunction.generateThisVariable(irClass: IrClass, initializer: IrExpression): IrVariable {
        return JsIrBuilder.buildVar(
            type = irClass.defaultType,
            parent = this,
            name = Namer.SYNTHETIC_RECEIVER_NAME,
            initializer = initializer
        )
    }

    private fun IrFunction.generateBoxParameter(irClass: IrClass): IrValueParameter {
        return JsIrBuilder.buildValueParameter(
            parent = this,
            name = "box",
            index = valueParameters.size,
            type = irClass.defaultType.makeNullable()
        )
    }

    private fun IrConstructor.hasStrictSignature(context: JsIrBackendContext): Boolean {
        return with(parentAsClass) { isExternal || isExpect || context.inlineClassesUtils.isClassInlineLike(this) }
    }

    private val IrClass.constructorPostfix: String
        get() = fqNameWhenAvailable?.asString()?.replace('.', '_') ?: name.toString()

    private val IrFunctionAccessExpression.typeArguments: List<IrType?>
        get() = List(typeArgumentsCount) { getTypeArgument(it) }

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

    private fun createJsObjectLiteral(): IrExpressionBody {
        return JsIrBuilder.buildCall(context.intrinsics.jsCall)
            .apply { putValueArgument(0, "{}".toIrConst(context.irBuiltIns.stringType)) }
            .let { IrExpressionBodyImpl(it) }
    }

    private fun IrBody.replaceSuperCallsAndThisUsages(
        irClass: IrClass,
        boxParameterSymbol: IrValueSymbol,
        constructorReplacement: IrSimpleFunction
    ): Pair<IrValueSymbol, Boolean> {
        var meetCapturing = false
        var generatedThisValueSymbol = boxParameterSymbol
        val selfParameterSymbol = irClass.thisReceiver!!.symbol

        transformChildrenVoid(object : ValueRemapper(emptyMap()) {
            override val map = mutableMapOf<IrValueSymbol, IrValueSymbol>(selfParameterSymbol to boxParameterSymbol)

            override fun visitSetField(expression: IrSetField): IrExpression {
                val receiver = expression.receiver as? IrGetValue
                if (receiver?.symbol == selfParameterSymbol) {
                    meetCapturing = true
                }
                return super.visitSetField(expression)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                val newThisValue = when {
                    expression.symbol.owner.isEffectivelyExternal() ->
                        JsIrBuilder.buildCall(context.intrinsics.jsCreateExternalThisSymbol)
                            .apply {
                                putValueArgument(0, JsIrBuilder.buildGetValue(constructorReplacement.dispatchReceiverParameter!!.symbol))
                                putValueArgument(1, expression.symbol.owner.parentAsClass.constructorRef)
                                putValueArgument(2, irAnyArray(expression.valueArguments))
                                putValueArgument(3, JsIrBuilder.buildGetValue(boxParameterSymbol))
                            }
                    else ->
                        JsIrBuilder.buildConstructorCall(
                            expression.symbol,
                            null,
                            expression.typeArguments,
                            ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT
                        ).apply {
                            copyValueArgumentsFrom(expression, expression.symbol.owner)
                        }
                }

                val newThisVariable = constructorReplacement.generateThisVariable(irClass, newThisValue)
                    .also {
                        map[selfParameterSymbol] = it.symbol
                        generatedThisValueSymbol = it.symbol
                    }

                return super.visitComposite(JsIrBuilder.buildComposite(context.dynamicType, listOf(newThisVariable)))
            }
        })

        return generatedThisValueSymbol to meetCapturing
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
