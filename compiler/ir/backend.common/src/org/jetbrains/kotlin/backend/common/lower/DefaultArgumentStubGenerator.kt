/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

// TODO: fix expect/actual default parameters

open class DefaultArgumentStubGenerator constructor(val context: CommonBackendContext, private val skipInlineMethods: Boolean = true) :
    DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction)
                lower(memberDeclaration)
            else
                null
        }

    }

    private val symbols = context.ir.symbols

    private fun lower(irFunction: IrFunction): List<IrFunction> {
        if (!irFunction.needsDefaultArgumentsLowering(skipInlineMethods))
            return listOf(irFunction)

        val bodies = irFunction.valueParameters.mapNotNull { it.defaultValue }


        log { "detected ${irFunction.name.asString()} has got #${bodies.size} default expressions" }

        if (bodies.isNotEmpty()) {

            val newIrFunction = irFunction.generateDefaultsFunction(context)
            newIrFunction.parent = irFunction.parent

            log { "$irFunction -> $newIrFunction" }
            val builder = context.createIrBuilder(newIrFunction.symbol)

            newIrFunction.body = builder.irBlockBody(newIrFunction) {
                val params = mutableListOf<IrVariable>()
                val variables = mutableMapOf<IrValueDeclaration, IrValueDeclaration>()

                irFunction.dispatchReceiverParameter?.let {
                    variables[it] = newIrFunction.dispatchReceiverParameter!!
                }

                irFunction.extensionReceiverParameter?.let {
                    variables[it] = newIrFunction.extensionReceiverParameter!!
                }

                for (valueParameter in irFunction.valueParameters) {
                    val parameter = newIrFunction.valueParameters[valueParameter.index]

                    val argument = if (valueParameter.defaultValue != null) {
                        val kIntAnd = symbols.intAnd.owner
                        val condition = irNotEquals(irCall(kIntAnd).apply {
                            dispatchReceiver = irGet(maskParameter(newIrFunction, valueParameter.index / 32))
                            putValueArgument(0, irInt(1 shl (valueParameter.index % 32)))
                        }, irInt(0))

                        val expressionBody = valueParameter.defaultValue!!

                        expressionBody.transformChildrenVoid(object : IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                log { "GetValue: ${expression.symbol.owner}" }
                                val valueSymbol = variables[expression.symbol.owner] ?: return expression
                                return irGet(valueSymbol)
                            }
                        })

                        irIfThenElse(
                            type = parameter.type,
                            condition = condition,
                            thenPart = expressionBody.expression,
                            elsePart = irGet(parameter)
                        )
                    } else {
                        irGet(parameter)
                    }

                    val temporaryVariable = irTemporary(argument, nameHint = parameter.name.asString())

                    params.add(temporaryVariable)
                    variables[valueParameter] = temporaryVariable
                }

                if (irFunction is IrConstructor) {
                    +IrDelegatingConstructorCallImpl(
                        startOffset = irFunction.startOffset,
                        endOffset = irFunction.endOffset,
                        type = context.irBuiltIns.unitType,
                        symbol = irFunction.symbol, descriptor = irFunction.symbol.descriptor,
                        typeArgumentsCount = irFunction.typeParameters.size
                    ).apply {
                        dispatchReceiver = newIrFunction.dispatchReceiverParameter?.let { irGet(it) }

                        params.forEachIndexed { i, variable -> putValueArgument(i, irGet(variable)) }
                    }
                } else {
                    +irReturn(irCall(irFunction).apply {
                        dispatchReceiver = newIrFunction.dispatchReceiverParameter?.let { irGet(it) }
                        extensionReceiver = newIrFunction.extensionReceiverParameter?.let { irGet(it) }

                        params.forEachIndexed { i, variable -> putValueArgument(i, irGet(variable)) }
                    })
                }
            }
            // Remove default argument initializers.
            irFunction.valueParameters.forEach {
                it.defaultValue = IrExpressionBodyImpl(IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.type, "Default Stub"))
            }

            return listOf(irFunction, newIrFunction)
        }
        return listOf(irFunction)
    }


    private fun log(msg: () -> String) = context.log { "DEFAULT-REPLACER: ${msg()}" }
}

private fun maskParameterDeclaration(function: IrFunction, number: Int) =
    maskParameter(function, number)

private fun maskParameter(function: IrFunction, number: Int) =
    function.valueParameters.single { it.name == parameterMaskName(number) }

private fun markerParameterDeclaration(function: IrFunction) =
    function.valueParameters.single { it.name == kConstructorMarkerName }

open class DefaultParameterInjector constructor(
    val context: CommonBackendContext,
    private val skipInline: Boolean = true
) : BodyLoweringPass {
    override fun lower(irBody: IrBody) {

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                super.visitDelegatingConstructorCall(expression)

                val declaration = expression.symbol.owner as IrFunction

                if (!declaration.needsDefaultArgumentsLowering(skipInline))
                    return expression

                val argumentsCount = argumentCount(expression)

                if (argumentsCount == declaration.valueParameters.size)
                    return expression

                val (symbolForCall, params) = parametersForCall(expression)
                symbolForCall as IrConstructorSymbol
                return IrDelegatingConstructorCallImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = context.irBuiltIns.unitType,
                    symbol = symbolForCall,
                    descriptor = symbolForCall.descriptor,
                    typeArgumentsCount = symbolForCall.owner.typeParameters.size
                )
                    .apply {
                        params.forEach {
                            log { "call::params@${it.first.index}/${it.first.name.asString()}: ${ir2string(it.second)}" }
                            putValueArgument(it.first.index, it.second)
                        }
                        dispatchReceiver = expression.dispatchReceiver
                    }

            }

            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val functionDeclaration = expression.symbol.owner

                if (!functionDeclaration.needsDefaultArgumentsLowering(skipInline))
                    return expression

                val argumentsCount = argumentCount(expression)
                if (argumentsCount == functionDeclaration.valueParameters.size)
                    return expression

                val (symbol, params) = parametersForCall(expression)
                val descriptor = symbol.descriptor
                val declaration = symbol.owner

                for (i in 0 until expression.typeArgumentsCount) {
                    log { "$descriptor [$i]: $expression.getTypeArgument(i)" }
                }
                declaration.typeParameters.forEach { log { "$declaration[${it.index}] : $it" } }

                return IrCallImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = symbol.owner.returnType,
                    symbol = symbol,
                    descriptor = descriptor,
                    typeArgumentsCount = expression.typeArgumentsCount
                )
                    .apply {
                        this.copyTypeArgumentsFrom(expression)

                        params.forEach {
                            log { "call::params@${it.first.index}/${it.first.name.asString()}: ${ir2string(it.second)}" }
                            putValueArgument(it.first.index, it.second)
                        }

                        dispatchReceiver = expression.dispatchReceiver
                        extensionReceiver = expression.extensionReceiver

                        log { "call::extension@: ${ir2string(expression.extensionReceiver)}" }
                        log { "call::dispatch@: ${ir2string(expression.dispatchReceiver)}" }
                    }
            }

            private fun IrFunction.findSuperMethodWithDefaultArguments(): IrFunction? {
                if (!needsDefaultArgumentsLowering(skipInline)) return null

                if (this !is IrSimpleFunction) return this

                for (s in overriddenSymbols) {
                    s.owner.findSuperMethodWithDefaultArguments()?.let { return it }
                }

                return this
            }

            private fun parametersForCall(expression: IrFunctionAccessExpression): Pair<IrFunctionSymbol, List<Pair<IrValueParameter, IrExpression?>>> {
                val declaration = expression.symbol.owner

                val keyFunction = declaration.findSuperMethodWithDefaultArguments()!!
                val realFunction = keyFunction.generateDefaultsFunction(context)

                realFunction.parent = keyFunction.parent

                log { "$declaration -> $realFunction" }
                val maskValues = Array((declaration.valueParameters.size + 31) / 32) { 0 }
                val params = mutableListOf<Pair<IrValueParameter, IrExpression?>>()
                params += declaration.valueParameters.mapIndexed { i, _ ->
                    val valueArgument = expression.getValueArgument(i)
                    if (valueArgument == null) {
                        val maskIndex = i / 32
                        maskValues[maskIndex] = maskValues[maskIndex] or (1 shl (i % 32))
                    }
                    val valueParameterDeclaration = realFunction.valueParameters[i]
                    val defaultValueArgument = if (valueParameterDeclaration.varargElementType != null) {
                        null
                    } else {
                        nullConst(expression, realFunction.valueParameters[i].type)
                    }
                    valueParameterDeclaration to (valueArgument ?: defaultValueArgument)
                }

                maskValues.forEachIndexed { i, maskValue ->
                    params += maskParameterDeclaration(realFunction, i) to IrConstImpl.int(
                        startOffset = irBody.startOffset,
                        endOffset = irBody.endOffset,
                        type = context.irBuiltIns.intType,
                        value = maskValue
                    )
                }
                if (expression.symbol is IrConstructorSymbol) {
                    val defaultArgumentMarker = context.ir.symbols.defaultConstructorMarker
                    params += markerParameterDeclaration(realFunction) to IrGetObjectValueImpl(
                        startOffset = irBody.startOffset,
                        endOffset = irBody.endOffset,
                        type = defaultArgumentMarker.owner.defaultType,
                        symbol = defaultArgumentMarker
                    )
                } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
                    params += realFunction.valueParameters.last() to
                            IrConstImpl.constNull(irBody.startOffset, irBody.endOffset, context.irBuiltIns.nothingNType)
                }
                params.forEach {
                    log { "descriptor::${realFunction.name.asString()}#${it.first.index}: ${it.first.name.asString()}" }
                }
                return Pair(realFunction.symbol, params)
            }

            private fun argumentCount(expression: IrMemberAccessExpression): Int {
                var result = 0
                for (i in 0 until expression.valueArgumentsCount) {
                    expression.getValueArgument(i)?.run { ++result }
                }
                return result
            }
        })
    }

    protected open fun nullConst(expression: IrElement, type: IrType) = when {
        type.isFloat() -> IrConstImpl.float(expression.startOffset, expression.endOffset, type, 0.0F)
        type.isDouble() -> IrConstImpl.double(expression.startOffset, expression.endOffset, type, 0.0)
        type.isBoolean() -> IrConstImpl.boolean(expression.startOffset, expression.endOffset, type, false)
        type.isByte() -> IrConstImpl.byte(expression.startOffset, expression.endOffset, type, 0)
        type.isChar() -> IrConstImpl.char(expression.startOffset, expression.endOffset, type, 0.toChar())
        type.isShort() -> IrConstImpl.short(expression.startOffset, expression.endOffset, type, 0)
        type.isInt() -> IrConstImpl.int(expression.startOffset, expression.endOffset, type, 0)
        type.isLong() -> IrConstImpl.long(expression.startOffset, expression.endOffset, type, 0)
        else -> IrConstImpl.constNull(expression.startOffset, expression.endOffset, context.irBuiltIns.nothingNType)
    }

    private fun log(msg: () -> String) = context.log { "DEFAULT-INJECTOR: ${msg()}" }
}

class DefaultParameterCleaner constructor(val context: CommonBackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        irFunction.valueParameters.forEach { it.defaultValue = null }
    }
}

private fun IrFunction.needsDefaultArgumentsLowering(skipInlineMethods: Boolean): Boolean {
    if (isInline && skipInlineMethods) return false
    if (valueParameters.any { it.defaultValue != null }) return true

    if (this !is IrSimpleFunction) return false

    return overriddenSymbols.any { it.owner.needsDefaultArgumentsLowering(skipInlineMethods) }
}

private fun IrFunction.generateDefaultsFunctionImpl(context: CommonBackendContext): IrFunction {
    val newFunction = buildFunctionDeclaration(this)

    val syntheticParameters = MutableList((valueParameters.size + 31) / 32) { i ->
        valueParameter(valueParameters.size + i, parameterMaskName(i), context.irBuiltIns.intType)
    }

    if (this is IrConstructor) {
        syntheticParameters += newFunction.valueParameter(
            syntheticParameters.last().index + 1,
            kConstructorMarkerName,
            context.ir.symbols.defaultConstructorMarker.owner.defaultType
        )
    } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
        syntheticParameters += newFunction.valueParameter(
            syntheticParameters.last().index + 1,
            "handler".synthesizedName,
            context.irBuiltIns.anyType
        )
    }

    val newValueParameters = valueParameters.map { it.copyTo(newFunction) } + syntheticParameters
    val newTypeParameters = typeParameters.map { it.copyTo(newFunction) }

    newFunction.returnType = returnType
    newFunction.dispatchReceiverParameter = dispatchReceiverParameter?.run {
        IrValueParameterImpl(startOffset, endOffset, origin, descriptor, type, varargElementType).also { it.parent = newFunction }
    }
    newFunction.extensionReceiverParameter = extensionReceiverParameter?.copyTo(newFunction)
    newFunction.valueParameters += newValueParameters
    newFunction.typeParameters += newTypeParameters

    annotations.mapTo(newFunction.annotations) { it.deepCopyWithSymbols() }

    return newFunction
}

private fun buildFunctionDeclaration(irFunction: IrFunction): IrFunction {
    when (irFunction) {
        is IrConstructor -> {
            val descriptor = WrappedClassConstructorDescriptor(irFunction.descriptor.annotations, irFunction.descriptor.source)
            return IrConstructorImpl(
                irFunction.startOffset,
                irFunction.endOffset,
                DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                IrConstructorSymbolImpl(descriptor),
                irFunction.name,
                irFunction.visibility,
                irFunction.isInline,
                false,
                false
            ).also {
                descriptor.bind(it)
                it.parent = irFunction.parent
            }
        }
        is IrSimpleFunction -> {
            val descriptor = WrappedSimpleFunctionDescriptor(irFunction.descriptor.annotations, irFunction.descriptor.source)
            val name = Name.identifier("${irFunction.name}\$default")

            return IrFunctionImpl(
                irFunction.startOffset,
                irFunction.endOffset,
                DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                IrSimpleFunctionSymbolImpl(descriptor),
                name,
                irFunction.visibility,
                Modality.FINAL,
                irFunction.isInline,
                false,
                false,
                irFunction.isSuspend
            ).also {
                descriptor.bind(it)
                it.parent = irFunction.parent
            }
        }
        else -> throw IllegalStateException("Unknown function type")
    }
}

private fun IrFunction.generateDefaultsFunction(context: CommonBackendContext): IrFunction =
    context.ir.defaultParameterDeclarationsCache.getOrPut(this) {
        generateDefaultsFunctionImpl(context)
    }

object DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER :
    IrDeclarationOriginImpl("DEFAULT_PARAMETER_EXTENT")

private fun IrFunction.valueParameter(index: Int, name: Name, type: IrType): IrValueParameter {
    val parameterDescriptor = WrappedValueParameterDescriptor()

    return IrValueParameterImpl(
        startOffset,
        endOffset,
        IrDeclarationOrigin.DEFINED,
        IrValueParameterSymbolImpl(parameterDescriptor),
        name,
        index,
        type,
        null,
        false,
        false
    ).also {
        parameterDescriptor.bind(it)
        it.parent = this
    }
}

internal val kConstructorMarkerName = "marker".synthesizedName

private fun parameterMaskName(number: Int) = "mask$number".synthesizedName
