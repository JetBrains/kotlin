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
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFunWithDescriptorForInlining
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

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
        val newIrFunction = irFunction.generateDefaultsFunction(context, skipInlineMethods, skipExternalMethods)
            ?: return listOf(irFunction)
        if (newIrFunction.origin == IrDeclarationOrigin.FAKE_OVERRIDE) {
            return listOf(irFunction, newIrFunction)
        }

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
        val argumentsCount = (0 until expression.valueArgumentsCount).count { expression.getValueArgument(it) != null }
        if (argumentsCount == expression.symbol.owner.valueParameters.size)
            return expression

        val (symbol, params) = parametersForCall(expression) ?: return expression
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
                it.owner.parentAsClass.typeParameters.size,
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

    private fun parametersForCall(
        expression: IrFunctionAccessExpression
    ): Pair<IrFunctionSymbol, List<Pair<IrValueParameter, IrExpression?>>>? {
        val declaration = expression.symbol.owner
        val keyFunction = declaration.generateDefaultsFunction(context, skipInline, skipExternalMethods) ?: return null
        val realFunction = if (keyFunction is IrSimpleFunction) keyFunction.resolveFakeOverride()!! else keyFunction

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

private fun IrFunction.generateDefaultsFunction(
    context: CommonBackendContext,
    skipInlineMethods: Boolean,
    skipExternalMethods: Boolean
): IrFunction? = when {
    skipInlineMethods && isInline -> null
    skipExternalMethods && isExternalOrInheritedFromExternal() -> null
    valueParameters.any { it.defaultValue != null } ->
        generateDefaultsFunctionImpl(context, IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER)
    this is IrSimpleFunction -> {
        // If this is an override of a function with default arguments, produce a fake override of a default stub.
        val overriddenStubs = overriddenSymbols.mapNotNull {
            it.owner.generateDefaultsFunction(context, skipInlineMethods, skipExternalMethods)?.symbol as IrSimpleFunctionSymbol?
        }
        if (overriddenStubs.isNotEmpty())
            generateDefaultsFunctionImpl(context, IrDeclarationOrigin.FAKE_OVERRIDE).also {
                (it as IrSimpleFunction).overriddenSymbols.addAll(overriddenStubs)
            }
        else
            null
    }
    else -> null
}

private fun IrFunction.generateDefaultsFunctionImpl(context: CommonBackendContext, newOrigin: IrDeclarationOrigin): IrFunction =
    context.ir.defaultParameterDeclarationsCache.getOrPut(this) {
        val newFunction = when (this) {
            is IrConstructor ->
                buildConstructor {
                    updateFrom(this@generateDefaultsFunctionImpl)
                    origin = newOrigin
                    isExternal = false
                    isPrimary = false
                    isExpect = false
                }
            is IrSimpleFunction ->
                buildFunWithDescriptorForInlining(descriptor) {
                    updateFrom(this@generateDefaultsFunctionImpl)
                    name = Name.identifier("${this@generateDefaultsFunctionImpl.name}\$default")
                    origin = newOrigin
                    modality = Modality.FINAL
                    isExternal = false
                    isTailrec = false
                }
            else -> throw IllegalStateException("Unknown function type")
        }
        newFunction.copyTypeParametersFrom(this)
        newFunction.parent = parent
        newFunction.returnType = returnType.remapTypeParameters(classIfConstructor, newFunction.classIfConstructor)
        newFunction.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(newFunction)
        newFunction.extensionReceiverParameter = extensionReceiverParameter?.copyTo(newFunction)

        valueParameters.mapTo(newFunction.valueParameters) {
            val newType = it.type.remapTypeParameters(classIfConstructor, newFunction.classIfConstructor)
            val makeNullable = it.defaultValue != null &&
                    (context.ir.unfoldInlineClassType(it.type) ?: it.type) !in context.irBuiltIns.primitiveIrTypes
            it.copyTo(newFunction, type = if (makeNullable) newType.makeNullable() else newType, defaultValue = null)
        }

        for (i in 0 until (valueParameters.size + 31) / 32) {
            newFunction.addValueParameter(parameterMaskName(i).asString(), context.irBuiltIns.intType)
        }
        if (this is IrConstructor) {
            newFunction.addValueParameter(
                kConstructorMarkerName.asString(),
                context.ir.symbols.defaultConstructorMarker.owner.defaultType.makeNullable()
            )
        } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
            newFunction.addValueParameter("handler".synthesizedString, context.irBuiltIns.anyNType)
        }

        // TODO some annotations are needed (e.g. @JvmStatic), others need different values (e.g. @JvmName), the rest are redundant.
        annotations.mapTo(newFunction.annotations) { it.deepCopyWithSymbols() }
        newFunction
    }

internal val kConstructorMarkerName = "marker".synthesizedName

private fun parameterMaskName(number: Int) = "mask$number".synthesizedName
