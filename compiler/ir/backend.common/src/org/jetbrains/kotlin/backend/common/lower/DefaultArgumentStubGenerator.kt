/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.Modality
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
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

// TODO: fix expect/actual default parameters

open class DefaultArgumentStubGenerator(
    open val context: CommonBackendContext,
    private val skipInlineMethods: Boolean = true,
    private val skipExternalMethods: Boolean = false
) : DeclarationContainerLoweringPass {

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.transformDeclarationsFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction)
                lower(memberDeclaration)
            else
                null
        }
    }

    private val symbols get() = context.ir.symbols

    private fun lower(irFunction: IrFunction): List<IrFunction> {
        if (!irFunction.needsDefaultArgumentsLowering(skipInlineMethods, skipExternalMethods))
            return listOf(irFunction)

        val bodies = irFunction.valueParameters.mapNotNull { it.defaultValue }


        log { "detected ${irFunction.name.asString()} has got #${bodies.size} default expressions" }

        if (bodies.isEmpty()) {
            // Fake override
            val newIrFunction = irFunction.generateDefaultsFunction(context, IrDeclarationOrigin.FAKE_OVERRIDE, skipInlineMethods, skipExternalMethods)

            return listOf(irFunction, newIrFunction)
        }

        val newIrFunction =
            irFunction.generateDefaultsFunction(context, IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER, skipInlineMethods, skipExternalMethods)

        log { "$irFunction -> $newIrFunction" }
        val builder = context.createIrBuilder(newIrFunction.symbol)

        newIrFunction.body = builder.irBlockBody(newIrFunction) {
            val params = mutableListOf<IrValueDeclaration>()
            val variables = mutableMapOf<IrValueDeclaration, IrValueDeclaration>()

            irFunction.dispatchReceiverParameter?.let {
                variables[it] = newIrFunction.dispatchReceiverParameter!!
            }

            irFunction.extensionReceiverParameter?.let {
                variables[it] = newIrFunction.extensionReceiverParameter!!
            }

            // In order to deal with forward references in default value lambdas,
            // accesses to the parameter before it has been determined if there is
            // a default value or not is redirected to the actual parameter of the
            // $default function. This is to ensure that examples such as:
            //
            // fun f(f1: () -> String = { f2() },
            //       f2: () -> String = { "OK" }) = f1()
            //
            // works correctly so that `f() { "OK" }` returns "OK" and
            // `f()` throws a NullPointerException.
            irFunction.valueParameters.associateWithTo(variables) {
                newIrFunction.valueParameters[it.index]
            }

            for (valueParameter in irFunction.valueParameters) {
                val parameter = newIrFunction.valueParameters[valueParameter.index]
                val remapped = if (valueParameter.defaultValue != null) {
                    val kIntAnd = symbols.intAnd.owner
                    val condition = irNotEquals(irCall(kIntAnd).apply {
                        dispatchReceiver = irGet(maskParameter(newIrFunction, valueParameter.index / 32))
                        putValueArgument(0, irInt(1 shl (valueParameter.index % 32)))
                    }, irInt(0))

                    val expressionBody = valueParameter.defaultValue!!
                    expressionBody.patchDeclarationParents(newIrFunction)
                    expressionBody.transformChildrenVoid(object : IrElementTransformerVoid() {
                        override fun visitGetValue(expression: IrGetValue): IrExpression {
                            log { "GetValue: ${expression.symbol.owner}" }
                            val valueSymbol = variables[expression.symbol.owner] ?: return expression
                            return irGet(valueSymbol)
                        }
                    })

                    selectArgumentOrDefault(condition, parameter, expressionBody.expression)
                } else {
                    parameter
                }
                params.add(remapped)
                variables[valueParameter] = remapped
            }

            when (irFunction) {
                is IrConstructor -> +IrDelegatingConstructorCallImpl(
                    startOffset = irFunction.startOffset,
                    endOffset = irFunction.endOffset,
                    type = context.irBuiltIns.unitType,
                    symbol = irFunction.symbol,
                    typeArgumentsCount = newIrFunction.parentAsClass.typeParameters.size + newIrFunction.typeParameters.size
                ).apply {
                    passTypeArgumentsFrom(newIrFunction.parentAsClass)
                    passTypeArgumentsFrom(newIrFunction)
                    dispatchReceiver = newIrFunction.dispatchReceiverParameter?.let { irGet(it) }
                    params.forEachIndexed { i, variable -> putValueArgument(i, irGet(variable)) }
                }
                is IrSimpleFunction -> +irReturn(dispatchToImplementation(irFunction, newIrFunction, params))
                else -> error("Unknown function declaration")
            }
        }
        // Remove default argument initializers.
        irFunction.valueParameters.forEach {
            if (it.defaultValue != null) {
                it.defaultValue = IrExpressionBodyImpl(IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.type, "Default Stub"))
            }
        }
        return listOf(irFunction, newIrFunction)
    }

    protected open fun IrBlockBodyBuilder.selectArgumentOrDefault(
        shouldUseDefault: IrExpression,
        parameter: IrValueParameter,
        default: IrExpression
    ): IrValueDeclaration {
        val value = irIfThenElse(parameter.type, shouldUseDefault, default, irGet(parameter))
        return createTmpVariable(value, nameHint = parameter.name.asString())
    }

    private fun IrBlockBodyBuilder.dispatchToImplementation(
        irFunction: IrSimpleFunction,
        newIrFunction: IrFunction,
        params: MutableList<IrValueDeclaration>
    ): IrExpression {
        val dispatchCall = irCall(irFunction.symbol).apply {
            passTypeArgumentsFrom(newIrFunction)
            dispatchReceiver = newIrFunction.dispatchReceiverParameter?.let { irGet(it) }
            extensionReceiver = newIrFunction.extensionReceiverParameter?.let { irGet(it) }

            params.forEachIndexed { i, variable -> putValueArgument(i, irGet(variable)) }
        }
        return if (needSpecialDispatch(irFunction)) {
            val handlerDeclaration = newIrFunction.valueParameters.last()
            // if $handler != null $handler(a, b, c) else foo(a, b, c)
            irIfThenElse(
                irFunction.returnType,
                irEqualsNull(irGet(handlerDeclaration)),
                dispatchCall,
                generateHandleCall(handlerDeclaration, irFunction, newIrFunction, params)
            )
        } else dispatchCall
    }

    protected open fun needSpecialDispatch(irFunction: IrSimpleFunction) = false
    protected open fun IrBlockBodyBuilder.generateHandleCall(
        handlerDeclaration: IrValueParameter,
        oldIrFunction: IrFunction,
        newIrFunction: IrFunction,
        params: MutableList<IrValueDeclaration>
    ): IrExpression {
        assert(needSpecialDispatch(oldIrFunction as IrSimpleFunction))
        error("This method should be overridden")
    }

    private fun log(msg: () -> String) = context.log { "DEFAULT-REPLACER: ${msg()}" }
}

private fun maskParameterDeclaration(function: IrFunction, number: Int) =
    maskParameter(function, number)

private fun maskParameter(function: IrFunction, number: Int) =
    function.valueParameters.single { it.name == parameterMaskName(number) }

private fun markerParameterDeclaration(function: IrFunction) =
    function.valueParameters.single { it.name == kConstructorMarkerName }

val DEFAULT_DISPATCH_CALL = object : IrStatementOriginImpl("DEFAULT_DISPATCH_CALL") {}

open class DefaultParameterInjector(
    open val context: CommonBackendContext,
    private val skipInline: Boolean = true,
    private val skipExternalMethods: Boolean = false
) : IrElementTransformerVoid(), BodyLoweringPass, FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(this)
    }

    private fun visitFunctionAccessExpression(
        expression: IrFunctionAccessExpression,
        builder: (IrFunctionSymbol) -> IrFunctionAccessExpression
    ): IrExpression {
        val functionDeclaration = expression.symbol.owner

        if (!functionDeclaration.needsDefaultArgumentsLowering(skipInline, skipExternalMethods))
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

        return builder(symbol).apply {
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

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        super.visitDelegatingConstructorCall(expression)

        return visitFunctionAccessExpression(expression) {
            IrDelegatingConstructorCallImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = context.irBuiltIns.unitType,
                symbol = it as IrConstructorSymbol,
                typeArgumentsCount = expression.typeArgumentsCount
            )
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        super.visitConstructorCall(expression)

        return visitFunctionAccessExpression(expression) {
            IrConstructorCallImpl.fromSymbolOwner(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                it as IrConstructorSymbol,
                DEFAULT_DISPATCH_CALL
            )
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
        super.visitEnumConstructorCall(expression)

        return visitFunctionAccessExpression(expression) {
            IrEnumConstructorCallImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                it as IrConstructorSymbol
            )
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        return visitFunctionAccessExpression(expression) {
            IrCallImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type,
                symbol = it,
                typeArgumentsCount = expression.typeArgumentsCount,
                origin = DEFAULT_DISPATCH_CALL,
                superQualifierSymbol = expression.superQualifierSymbol
            )
        }
    }

    private fun IrFunction.findSuperMethodWithDefaultArguments(): IrFunction? {
        if (!needsDefaultArgumentsLowering(skipInline, skipExternalMethods)) return null

        if (this !is IrSimpleFunction) return this

        for (s in overriddenSymbols) {
            s.owner.findSuperMethodWithDefaultArguments()?.let { return it }
        }

        return this
    }

    private fun parametersForCall(
        expression: IrFunctionAccessExpression
    ): Pair<IrFunctionSymbol, List<Pair<IrValueParameter, IrExpression?>>> {
        val declaration = expression.symbol.owner

        val keyFunction = declaration.findSuperMethodWithDefaultArguments()!!
        val realFunction =
            keyFunction.generateDefaultsFunction(
                context,
                IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER,
                skipInline,
                skipExternalMethods
            )

        log { "$declaration -> $realFunction" }
        val maskValues = Array((declaration.valueParameters.size + 31) / 32) { 0 }
        val params = mutableListOf<Pair<IrValueParameter, IrExpression?>>()
        params += declaration.valueParameters.mapIndexed { argIndex, _ ->
            val valueArgument = expression.getValueArgument(argIndex)
            if (valueArgument == null) {
                val maskIndex = argIndex / 32
                maskValues[maskIndex] = maskValues[maskIndex] or (1 shl (argIndex % 32))
            }
            val valueParameterDeclaration = realFunction.valueParameters[argIndex]
            val defaultValueArgument = nullConst(expression.startOffset, expression.endOffset, valueParameterDeclaration)
            valueParameterDeclaration to (valueArgument ?: defaultValueArgument)
        }

        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        maskValues.forEachIndexed { i, maskValue ->
            params += maskParameterDeclaration(realFunction, i) to IrConstImpl.int(
                startOffset = startOffset,
                endOffset = endOffset,
                type = context.irBuiltIns.intType,
                value = maskValue
            )
        }
        if (expression.symbol is IrConstructorSymbol) {
            val defaultArgumentMarker = context.ir.symbols.defaultConstructorMarker
            params += markerParameterDeclaration(realFunction) to
                    IrConstImpl.constNull(startOffset, endOffset, defaultArgumentMarker.owner.defaultType.makeNullable())
        } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
            params += realFunction.valueParameters.last() to
                    IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)
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

    protected open fun nullConst(startOffset: Int, endOffset: Int, irParameter: IrValueParameter): IrExpression? =
        if (irParameter.varargElementType != null) {
            null
        } else {
            nullConst(startOffset, endOffset, irParameter.type)
        }

    protected open fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression =
        IrConstImpl.defaultValueForType(startOffset, endOffset, type)

    private fun log(msg: () -> String) = context.log { "DEFAULT-INJECTOR: ${msg()}" }
}

class DefaultParameterCleaner constructor(val context: CommonBackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        if (!context.scriptMode) {
            irFunction.valueParameters.forEach { it.defaultValue = null }
        }
    }
}

// TODO this implementation is exponential
private fun IrFunction.needsDefaultArgumentsLowering(skipInlineMethods: Boolean, skipExternalMethods: Boolean): Boolean {
    if (isInline && skipInlineMethods) return false
    if (skipExternalMethods && isExternalOrInheritedFromExternal()) return false
    if (valueParameters.any { it.defaultValue != null }) return true

    if (this !is IrSimpleFunction) return false

    fun IrSimpleFunction.inheritsDefaultValues(): Boolean =
        valueParameters.any { it.defaultValue != null } || overriddenSymbols.any { it.owner.inheritsDefaultValues() }

    return inheritsDefaultValues()
}

private fun IrFunction.generateDefaultsFunctionImpl(
    context: CommonBackendContext,
    origin: IrDeclarationOrigin,
    skipInlineMethods: Boolean,
    skipExternalMethods: Boolean
): IrFunction {
    val newFunction = buildFunctionDeclaration(this, origin)

    val syntheticParameters = MutableList((valueParameters.size + 31) / 32) { i ->
        newFunction.valueParameter(valueParameters.size + i, parameterMaskName(i), context.irBuiltIns.intType)
    }

    if (this is IrConstructor) {
        syntheticParameters += newFunction.valueParameter(
            syntheticParameters.last().index + 1,
            kConstructorMarkerName,
            context.ir.symbols.defaultConstructorMarker.owner.defaultType.makeNullable()
        )
    } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
        syntheticParameters += newFunction.valueParameter(
            syntheticParameters.last().index + 1,
            "handler".synthesizedName,
            context.irBuiltIns.anyNType
        )
    }

    newFunction.copyTypeParametersFrom(this)
    val newValueParameters = valueParameters.map { it.copyMaybeNullableTo(newFunction, context) } + syntheticParameters
    newValueParameters.forEach {
        it.defaultValue = null
    }

    newFunction.returnType = returnType
    newFunction.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(newFunction)
    newFunction.extensionReceiverParameter = extensionReceiverParameter?.copyTo(newFunction)
    newFunction.valueParameters += newValueParameters

    annotations.mapTo(newFunction.annotations) { it.deepCopyWithSymbols() }

    if (origin == IrDeclarationOrigin.FAKE_OVERRIDE) {
        for (baseFunSymbol in (this as IrSimpleFunction).overriddenSymbols) {
            val baseFun = baseFunSymbol.owner
            if (baseFun.needsDefaultArgumentsLowering(skipInlineMethods, skipExternalMethods)) {
                val baseOrigin = if (baseFun.valueParameters.any { it.defaultValue != null }) {
                    IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER
                } else {
                    IrDeclarationOrigin.FAKE_OVERRIDE
                }
                val defaultsBaseFun = baseFun.generateDefaultsFunction(context, baseOrigin, skipInlineMethods, skipExternalMethods)
                (newFunction as IrSimpleFunction).overriddenSymbols.add((defaultsBaseFun as IrSimpleFunction).symbol)
            }
        }
    }

    return newFunction
}

private fun buildFunctionDeclaration(irFunction: IrFunction, origin: IrDeclarationOrigin): IrFunction {
    when (irFunction) {
        is IrConstructor -> {
            val descriptor = WrappedClassConstructorDescriptor()
            return IrConstructorImpl(
                irFunction.startOffset,
                irFunction.endOffset,
                origin,
                IrConstructorSymbolImpl(descriptor),
                irFunction.name,
                irFunction.visibility,
                irFunction.returnType,
                isInline = irFunction.isInline,
                isExternal = false,
                isPrimary = false,
                isExpect = false
            ).also {
                descriptor.bind(it)
                it.parent = irFunction.parent
            }
        }
        is IrSimpleFunction -> {
            val descriptor = irFunction.descriptor.safeAs<DescriptorWithContainerSource>()?.let {
                WrappedFunctionDescriptorWithContainerSource(it.containerSource)
            } ?: WrappedSimpleFunctionDescriptor()
            val name = Name.identifier("${irFunction.name}\$default")

            return IrFunctionImpl(
                irFunction.startOffset,
                irFunction.endOffset,
                origin,
                IrSimpleFunctionSymbolImpl(descriptor),
                name,
                irFunction.visibility,
                Modality.FINAL,
                irFunction.returnType,
                isInline = irFunction.isInline,
                isExternal = false,
                isTailrec = false,
                isSuspend = irFunction.isSuspend,
                isExpect = irFunction.isExpect,
                isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
            ).also {
                descriptor.bind(it)
                it.parent = irFunction.parent
            }
        }
        else -> throw IllegalStateException("Unknown function type")
    }
}

private fun IrFunction.generateDefaultsFunction(
    context: CommonBackendContext,
    origin: IrDeclarationOrigin,
    skipInlineMethods: Boolean,
    skipExternalMethods: Boolean
): IrFunction =
    context.ir.defaultParameterDeclarationsCache.getOrPut(this) {
        generateDefaultsFunctionImpl(context, origin, skipInlineMethods, skipExternalMethods)
    }

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
        isCrossinline = false,
        isNoinline = false
    ).also {
        parameterDescriptor.bind(it)
        it.parent = this
    }
}

internal val kConstructorMarkerName = "marker".synthesizedName

private fun parameterMaskName(number: Int) = "mask$number".synthesizedName

private fun IrValueParameter.copyMaybeNullableTo(irFunction: IrFunction, context: CommonBackendContext): IrValueParameter {
    if (defaultValue == null) return copyTo(irFunction)
    val underlyingType = context.ir.unfoldInlineClassType(type) ?: type
    if (underlyingType in context.irBuiltIns.primitiveIrTypes) return copyTo(irFunction)

    val newType = type.remapTypeParameters(
        (parent as IrTypeParametersContainer).classIfConstructor,
        irFunction.classIfConstructor
    ).makeNullable()

    val descriptor = WrappedValueParameterDescriptor(symbol.descriptor.annotations, symbol.descriptor.source)
    return IrValueParameterImpl(
        startOffset, endOffset, origin, IrValueParameterSymbolImpl(descriptor),
        name, index, newType, varargElementType, isCrossinline, isNoinline
    ).also {
        descriptor.bind(it)
        it.parent = irFunction
        it.defaultValue = null
        it.annotations.addAll(annotations.map { it.deepCopyWithSymbols() })
    }
}

