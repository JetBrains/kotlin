/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.backend.js.utils.jsConstructorReference
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private object ES6_BOX_PARAMETER : IrDeclarationOriginImpl("ES6_BOX_PARAMETER")
private object ES6_INIT_CALL : IrStatementOriginImpl("ES6_INIT_CALL")
private object ES6_CONSTRUCTOR_REPLACEMENT : IrDeclarationOriginImpl("ES6_CONSTRUCTOR_REPLACEMENT")
private object ES6_INIT_FUNCTION : IrDeclarationOriginImpl("ES6_INIT_FUNCTION")
private object ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT : IrStatementOriginImpl("ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT")

val IrDeclaration.isSyntheticEs6Constructor: Boolean
    get() = origin == ES6_CONSTRUCTOR_REPLACEMENT

val IrFunctionAccessExpression.isSyntheticDelegatingReplacement: Boolean
    get() = origin == ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT

val IrDeclaration.isInitFunction: Boolean
    get() = origin == ES6_INIT_FUNCTION

val IrFunctionAccessExpression.isInitCall: Boolean
    get() = origin == ES6_INIT_CALL

val IrValueParameter.isBoxParameter: Boolean
    get() = origin == ES6_BOX_PARAMETER

class ES6ConstructorCallLowering(override val context: JsIrBackendContext) : ES6ClassOptimizationReceiver(), BodyLoweringPass {
    private var IrConstructor.constructorFactory by context.mapping.secondaryConstructorToFactory

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!context.es6mode) return

        val containerFunction = container as? IrFunction

        irBody.accept(object : IrElementTransformerVoidWithContext() {
            private val IrFunction?.boxParameter: IrValueParameter?
                get() = this?.valueParameters?.lastOrNull()?.takeIf { it.isBoxParameter }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                if (expression.symbol.owner.valueParameters.lastOrNull()?.isBoxParameter == true) {
                    val currentFunction = currentFunction?.irElement as? IrFunction ?: containerFunction
                    val boxParameter = currentFunction.boxParameter ?: return super.visitDelegatingConstructorCall(expression)

                    return IrDelegatingConstructorCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        expression.symbol,
                        expression.typeArgumentsCount,
                        expression.valueArgumentsCount + 1
                    ).apply {
                        copyTypeAndValueArgumentsFrom(expression)
                        putValueArgument(expression.valueArgumentsCount, JsIrBuilder.buildGetValue(boxParameter.symbol))
                    }
                }

                return super.visitDelegatingConstructorCall(expression)
            }

            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                val currentConstructor = expression.symbol.owner
                val irClass = currentConstructor.parentAsClass
                val currentFunction = currentFunction?.irElement as? IrFunction ?: containerFunction

                if (
                    currentConstructor.hasStrictSignature(context) ||
                    irClass.symbol == context.irBuiltIns.anyClass ||
                    currentConstructor.canBeTranslatedAsRegularConstructor
                ) {
                    return super.visitConstructorCall(expression)
                }

                val factoryFunction = currentConstructor.constructorFactory ?: error("Replacement for the constructor is not found")

                if (expression.isInitCall) {
                    assert(factoryFunction.isInitFunction) { "Expect to have init function replacement" }
                    return JsIrBuilder.buildCall(factoryFunction.symbol).apply {
                        copyValueArgumentsFrom(expression, factoryFunction)
                    }
                }

                val superQualifier = irClass.symbol.takeIf {
                    expression.isSyntheticDelegatingReplacement &&
                            currentFunction != null &&
                            currentFunction.parentAsClass != irClass
                }

                val factoryFunctionCall = JsIrBuilder.buildCall(
                    factoryFunction.symbol,
                    superQualifierSymbol = superQualifier,
                    origin = ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT
                ).apply {
                    copyValueArgumentsFrom(expression, factoryFunction)

                    if (!expression.isSyntheticDelegatingReplacement) {
                        dispatchReceiver = irClass.jsConstructorReference(context)
                    } else {
                        if (superQualifier == null) {
                            dispatchReceiver = JsIrBuilder.buildGetValue(factoryFunction.dispatchReceiverParameter!!.symbol)
                        }

                        currentFunction.boxParameter?.let {
                            putValueArgument(valueArgumentsCount - 1, JsIrBuilder.buildGetValue(it.symbol))
                        }
                    }
                }

                return visitCall(factoryFunctionCall)
            }
        }, null)
    }
}

class ES6AddBoxParameterToConstructorsLowering(override val context: JsIrBackendContext) : ES6ClassOptimizationReceiver(),
    DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrConstructor || declaration.hasStrictSignature(context)) return null

        hackEnums(declaration)
        hackExceptions(declaration)
        hackSimpleClassWithCapturing(declaration)

        if (declaration.needBoxParameter && !declaration.isSyntheticPrimaryConstructor) {
            declaration.addBoxParameter()
        }

        return null
    }

    private fun IrConstructor.addBoxParameter() {
        val irClass = parentAsClass
        val body = body as? IrBlockBody ?: return
        val boxParameter = generateBoxParameter(irClass).also { valueParameters += it }
        val isBoxUsed = body.replaceThisWithBoxBeforeSuperCall(irClass, boxParameter.symbol)
        if (isBoxUsed) {
            assert(needBoxParameter) { "Optimization of box parameter determinate that it is not required, but it's required!" }
            body.statements.add(0, boxParameter.generateDefaultResolution())
        }

        if (irClass.superClass == null && canBeTranslatedAsRegularConstructor) {
            val delegatingConstructorCall = body.statements.indexOfFirst { it is IrDelegatingConstructorCall }
            body.statements.add(delegatingConstructorCall + 1, JsIrBuilder.buildCall(context.intrinsics.jsBoxApplySymbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irClass.thisReceiver!!.symbol))
                putValueArgument(1, JsIrBuilder.buildGetValue(boxParameter.symbol))
            })
        }
    }

    private fun createJsObjectLiteral(): IrExpression {
        return JsIrBuilder.buildCall(context.intrinsics.jsEmptyObject)
    }

    private fun IrConstructor.generateBoxParameter(irClass: IrClass): IrValueParameter {
        return JsIrBuilder.buildValueParameter(
            parent = this,
            name = "box",
            index = valueParameters.size,
            type = irClass.defaultType.makeNullable(),
            origin = ES6_BOX_PARAMETER,
            isAssignable = true
        )
    }

    private fun IrValueParameter.generateDefaultResolution(): IrExpression {
        return with(context.createIrBuilder(symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)) {
            irIfThen(
                context.irBuiltIns.unitType,
                irEqeqeqWithoutBox(irGet(type, symbol), this@ES6AddBoxParameterToConstructorsLowering.context.getVoid()),
                irSet(symbol, createJsObjectLiteral())
            )
        }
    }

    private fun IrBody.replaceThisWithBoxBeforeSuperCall(irClass: IrClass, boxParameterSymbol: IrValueSymbol): Boolean {
        var meetCapturing = false
        var meetDelegatingConstructor = false
        val selfParameterSymbol = irClass.thisReceiver!!.symbol

        transformChildrenVoid(object : ValueRemapper(mapOf(selfParameterSymbol to boxParameterSymbol)) {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return if (meetDelegatingConstructor) {
                    expression
                } else {
                    super.visitGetValue(expression)
                }
            }

            override fun visitSetField(expression: IrSetField): IrExpression {
                if (meetDelegatingConstructor) return expression
                val newExpression = super.visitSetField(expression)
                val receiver = expression.receiver as? IrGetValue

                if (receiver?.symbol == boxParameterSymbol) {
                    meetCapturing = true
                }

                return newExpression
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                meetDelegatingConstructor = true
                return super.visitDelegatingConstructorCall(expression)
            }
        })

        return meetCapturing
    }

    private fun hackEnums(constructor: IrConstructor) {
        constructor.transformChildren(object : IrElementTransformerVoid() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                return (expression.argument as? IrDelegatingConstructorCall) ?: expression
            }
        }, null)
    }

    private fun hackSimpleClassWithCapturing(constructor: IrConstructor) {
        val irClass = constructor.parentAsClass
        if (irClass.superClass != null || (!irClass.isInner && !irClass.isLocal)) return
        val statements = (constructor.body as IrBlockBody).statements
        val delegationConstructorIndex = statements.indexOfFirst { it is IrDelegatingConstructorCall }
        if (delegationConstructorIndex == -1) return
        statements.add(0, statements[delegationConstructorIndex])
        statements.removeAt(delegationConstructorIndex + 1)
    }

    /**
     * Swap call synthetic primary ctor and call extendThrowable
     */
    private fun hackExceptions(constructor: IrConstructor) {
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
}

class ES6SecondaryConstructorLowering(override val context: JsIrBackendContext) : ES6ClassOptimizationReceiver(), DeclarationTransformer {
    private var IrConstructor.constructorFactory by context.mapping.secondaryConstructorToFactory

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrConstructor || declaration.hasStrictSignature(context)) return null

        if (declaration.canBeTranslatedAsRegularConstructor) {
            return null
        }

        return if (declaration.isSyntheticPrimaryConstructor) {
            listOf(declaration.generateInitFunction())
        } else {
            val factoryFunction = declaration.generateCreateFunction()
            listOfNotNull(factoryFunction, declaration.generateExportedConstructorIfNeed(factoryFunction))
        }
    }

    private fun IrConstructor.generateExportedConstructorIfNeed(factoryFunction: IrSimpleFunction): IrConstructor? {
        return runIf(isExported(context) && isPrimary && !canBeTranslatedAsRegularConstructor) {
            apply {
                body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                    val selfReplacedConstructorCall = JsIrBuilder.buildCall(factoryFunction.symbol).apply {
                        valueParameters.forEachIndexed { i, it ->
                            putValueArgument(i, JsIrBuilder.buildGetValue(it.symbol))
                        }
                        dispatchReceiver = JsIrBuilder.buildCall(context.intrinsics.jsNewTarget)
                    }
                    statements.add(JsIrBuilder.buildReturn(symbol, selfReplacedConstructorCall, returnType))
                }
            }
        }
    }

    private fun IrConstructor.generateInitFunction(): IrSimpleFunction {
        val constructor = this
        val irClass = parentAsClass
        val constructorName = "init_${irClass.constructorPostfix}"
        return context.irFactory.buildFun {
            name = Name.identifier(constructorName)
            returnType = context.irBuiltIns.unitType
            visibility = DescriptorVisibilities.PRIVATE
            modality = Modality.FINAL
            isInline = constructor.isInline
            isExternal = constructor.isExternal
            origin = ES6_INIT_FUNCTION
        }.also { factory ->
            factory.parent = irClass
            factory.copyTypeParametersFrom(irClass)
            factory.annotations = annotations
            factory.extensionReceiverParameter = irClass.thisReceiver?.copyTo(factory)

            factory.body = constructor.body?.deepCopyWithSymbols(factory)?.apply {
                transformChildrenVoid(ValueRemapper(mapOf(irClass.thisReceiver!!.symbol to factory.extensionReceiverParameter!!.symbol)))
            }

            constructorFactory = factory
        }
    }

    private fun IrConstructor.generateCreateFunction(): IrSimpleFunction {
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
            factory.copyValueParametersFrom(constructor)
            factory.annotations = annotations
            factory.dispatchReceiverParameter = irClass.thisReceiver?.copyTo(factory)

            if (irClass.isExported(context) && constructor.isPrimary) {
                factory.excludeFromExport()
            }

            factory.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                val bodyCopy = constructor.body?.deepCopyWithSymbols(factory) ?: return@createBlockBody
                val self = bodyCopy.replaceSuperCallsAndThisUsages(irClass, factory, constructor)
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

    private val IrClass.constructorPostfix: String
        get() = fqNameWhenAvailable?.asString()?.replace('.', '_') ?: name.toString()

    private val IrFunctionAccessExpression.typeArguments: List<IrType?>
        get() = List(typeArgumentsCount) { getTypeArgument(it) }

    private val IrFunctionAccessExpression.valueArguments: List<IrExpression>
        get() = List(valueArgumentsCount) { getValueArgument(it) ?: context.getVoid() }

    private fun irAnyArray(elements: List<IrExpression>): IrExpression {
        return JsIrBuilder.buildArray(
            elements,
            context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.anyNType),
            context.irBuiltIns.anyNType,
        )
    }

    private fun IrBody.replaceSuperCallsAndThisUsages(
        irClass: IrClass,
        constructorReplacement: IrSimpleFunction,
        currentConstructor: IrConstructor,
    ): IrValueSymbol {
        var generatedThisValueSymbol: IrValueSymbol? = null
        val selfParameterSymbol = irClass.thisReceiver!!.symbol
        val isOptimizedConstructor = currentConstructor.canBeTranslatedAsRegularConstructor

        val boxParameterSymbol = runIf(currentConstructor.needBoxParameter) {
            constructorReplacement.valueParameters.find { it.isBoxParameter }
        }

        transformChildrenVoid(object : ValueRemapper(emptyMap()) {
            override val map: MutableMap<IrValueSymbol, IrValueSymbol> = currentConstructor.valueParameters
                .zip(constructorReplacement.valueParameters)
                .associate { it.first.symbol to it.second.symbol }
                .toMutableMap()

            override fun visitReturn(expression: IrReturn): IrExpression {
                return if (expression.returnTargetSymbol == currentConstructor.symbol) {
                    super.visitReturn(
                        JsIrBuilder.buildReturn(
                            constructorReplacement.symbol,
                            JsIrBuilder.buildGetValue(selfParameterSymbol),
                            irClass.defaultType
                        )
                    )
                } else {
                    super.visitReturn(expression)
                }
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                val constructor = expression.symbol.owner

                if (isOptimizedConstructor) return super.visitDelegatingConstructorCall(expression)

                if (constructor.isSyntheticPrimaryConstructor) {
                    return JsIrBuilder.buildConstructorCall(expression.symbol, origin = ES6_INIT_CALL)
                        .apply {
                            copyValueArgumentsFrom(expression, constructor)
                            extensionReceiver = JsIrBuilder.buildGetValue(selfParameterSymbol)
                        }
                        .run { visitConstructorCall(this) }
                }

                val boxParameterGetter = boxParameterSymbol?.let { JsIrBuilder.buildGetValue(it.symbol) } ?: context.getVoid()

                val newThisValue = when {
                    constructor.isEffectivelyExternal() ->
                        JsIrBuilder.buildCall(context.intrinsics.jsCreateExternalThisSymbol)
                            .apply {
                                putValueArgument(0, irClass.getCurrentConstructorReference(constructorReplacement))
                                putValueArgument(1, expression.symbol.owner.parentAsClass.jsConstructorReference(context))
                                putValueArgument(2, irAnyArray(expression.valueArguments))
                                putValueArgument(3, boxParameterGetter)
                            }
                    constructor.parentAsClass.symbol == context.irBuiltIns.anyClass ->
                        JsIrBuilder.buildCall(context.intrinsics.jsCreateThisSymbol)
                            .apply {
                                putValueArgument(0, irClass.getCurrentConstructorReference(constructorReplacement))
                                putValueArgument(1, boxParameterGetter)
                            }
                    else ->
                        JsIrBuilder.buildConstructorCall(
                            expression.symbol,
                            null,
                            expression.typeArguments,
                            ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT
                        ).apply {
                            copyValueArgumentsFrom(expression, constructor)
                        }
                }

                val newThisVariable = constructorReplacement.generateThisVariable(irClass, newThisValue)
                    .also {
                        generatedThisValueSymbol = it.symbol
                        map[selfParameterSymbol] = it.symbol
                    }

                return super.visitComposite(JsIrBuilder.buildComposite(context.irBuiltIns.unitType, listOf(newThisVariable)))
            }
        })

        return generatedThisValueSymbol!!
    }

    private fun IrClass.getCurrentConstructorReference(currentFactoryFunction: IrSimpleFunction): IrExpression {
        return if (isFinalClass) {
            jsConstructorReference(context)
        } else {
            JsIrBuilder.buildGetValue(currentFactoryFunction.dispatchReceiverParameter!!.symbol)
        }
    }

    private fun IrDeclaration.excludeFromExport() {
        val jsExportIgnoreClass = context.intrinsics.jsExportIgnoreAnnotationSymbol.owner
        val jsExportIgnoreCtor = jsExportIgnoreClass.primaryConstructor ?: return
        annotations += JsIrBuilder.buildConstructorCall(jsExportIgnoreCtor.symbol)
    }
}

abstract class ES6ClassOptimizationReceiver {
    abstract val context: JsIrBackendContext

    protected val IrConstructor.canBeTranslatedAsRegularConstructor: Boolean
        get() = !context.incrementalCacheEnabled &&
                isPrimary &&
                context.mapping.esClassToPossibilityForOptimization[parentAsClass]?.value == true

    protected val IrConstructor.needBoxParameter: Boolean
        get() = context.incrementalCacheEnabled || context.mapping.esClassWhichNeedBoxParameters.contains(parentAsClass)
}


private fun IrConstructor.hasStrictSignature(context: JsIrBackendContext): Boolean {
    val primitives = with(context.irBuiltIns) { primitiveTypesToPrimitiveArrays.values + stringClass }
    return with(parentAsClass) {
        isExternal || isExpect || context.inlineClassesUtils.isClassInlineLike(this) || symbol in primitives
    }
}

