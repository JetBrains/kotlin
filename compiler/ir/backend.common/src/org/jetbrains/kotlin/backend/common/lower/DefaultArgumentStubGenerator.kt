/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg

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
        val functionDescriptor = irFunction.descriptor

        if (!functionDescriptor.needsDefaultArgumentsLowering(skipInlineMethods))
            return listOf(irFunction)

        val bodies = functionDescriptor.valueParameters
            .mapNotNull { irFunction.getDefault(it) }


        log { "detected ${functionDescriptor.name.asString()} has got #${bodies.size} default expressions" }
        functionDescriptor.overriddenDescriptors.forEach { context.log { "DEFAULT-REPLACER: $it" } }
        if (bodies.isNotEmpty()) {
            val newIrFunction = irFunction.generateDefaultsFunction(context)
            newIrFunction.parent = irFunction.parent
            val descriptor = newIrFunction.descriptor
            log { "$functionDescriptor -> $descriptor" }
            val builder = context.createIrBuilder(newIrFunction.symbol)
            newIrFunction.body = builder.irBlockBody(newIrFunction) {
                val params = mutableListOf<IrVariable>()
                val variables = mutableMapOf<ValueDescriptor, IrValueDeclaration>()

                irFunction.dispatchReceiverParameter?.let {
                    variables[it.descriptor] = newIrFunction.dispatchReceiverParameter!!
                }

                if (descriptor.extensionReceiverParameter != null) {
                    variables[functionDescriptor.extensionReceiverParameter!!] =
                            newIrFunction.extensionReceiverParameter!!
                }

                for (valueParameter in functionDescriptor.valueParameters) {
                    val parameter = newIrFunction.valueParameters[valueParameter.index]

                    val argument = if (valueParameter.hasDefaultValue()) {
                        val kIntAnd = symbols.intAnd.owner
                        val condition = irNotEquals(irCall(kIntAnd).apply {
                            dispatchReceiver = irGet(maskParameter(newIrFunction, valueParameter.index / 32))
                            putValueArgument(0, irInt(1 shl (valueParameter.index % 32)))
                        }, irInt(0))
                        val expressionBody = getDefaultParameterExpressionBody(irFunction, valueParameter)

                        /* Use previously calculated values in next expression. */
                        expressionBody.transformChildrenVoid(object : IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                log { "GetValue: ${expression.descriptor}" }
                                val valueSymbol = variables[expression.descriptor] ?: return expression
                                return irGet(valueSymbol)
                            }
                        })
                        irIfThenElse(
                            type = parameter.type,
                            condition = condition,
                            thenPart = expressionBody.expression,
                            elsePart = irGet(parameter)
                        )

                        /* Mapping calculated values with its origin variables. */
                    } else {
                        irGet(parameter)
                    }
                    val temporaryVariable = irTemporary(argument, nameHint = parameter.name.asString())

                    params.add(temporaryVariable)
                    variables.put(valueParameter, temporaryVariable)
                }
                if (irFunction is IrConstructor) {
                    +IrDelegatingConstructorCallImpl(
                        startOffset = irFunction.startOffset,
                        endOffset = irFunction.endOffset,
                        type = context.irBuiltIns.unitType,
                        symbol = irFunction.symbol, descriptor = irFunction.symbol.descriptor,
                        typeArgumentsCount = irFunction.typeParameters.size
                    ).apply {
                        params.forEachIndexed { i, variable ->
                            putValueArgument(i, irGet(variable))
                        }
                        if (functionDescriptor.dispatchReceiverParameter != null) {
                            dispatchReceiver = irGet(newIrFunction.dispatchReceiverParameter!!)
                        }
                    }
                } else {
                    +irReturn(irCall(irFunction).apply {
                        if (functionDescriptor.dispatchReceiverParameter != null) {
                            dispatchReceiver = irGet(newIrFunction.dispatchReceiverParameter!!)
                        }
                        if (functionDescriptor.extensionReceiverParameter != null) {
                            extensionReceiver = irGet(variables[functionDescriptor.extensionReceiverParameter!!]!!)
                        }
                        params.forEachIndexed { i, variable ->
                            putValueArgument(i, irGet(variable))
                        }
                    })
                }
            }
            // Remove default argument initializers.
            irFunction.valueParameters.forEach {
                it.defaultValue = null
            }

            return listOf(irFunction, newIrFunction)
        }
        return listOf(irFunction)
    }


    private fun log(msg: () -> String) = context.log { "DEFAULT-REPLACER: ${msg()}" }
}

private fun getDefaultParameterExpressionBody(irFunction: IrFunction, valueParameter: ValueParameterDescriptor): IrExpressionBody {
    return irFunction.getDefault(valueParameter) ?: TODO("FIXME!!!")
}

private fun maskParameterDescriptor(function: IrFunction, number: Int) =
    maskParameter(function, number).descriptor as ValueParameterDescriptor

private fun maskParameter(function: IrFunction, number: Int) =
    function.valueParameters.single { it.descriptor.name == parameterMaskName(number) }

private fun markerParameterDescriptor(descriptor: FunctionDescriptor) =
    descriptor.valueParameters.single { it.name == kConstructorMarkerName }

open class DefaultParameterInjector constructor(
    val context: CommonBackendContext,
    private val skipInline: Boolean = true
) : BodyLoweringPass {
    override fun lower(irBody: IrBody) {

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                super.visitDelegatingConstructorCall(expression)
                val descriptor = expression.descriptor
                if (!descriptor.needsDefaultArgumentsLowering(skipInline))
                    return expression
                val argumentsCount = argumentCount(expression)
                if (argumentsCount == descriptor.valueParameters.size)
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
                val functionDescriptor = expression.descriptor

                if (!functionDescriptor.needsDefaultArgumentsLowering(skipInline))
                    return expression

                val argumentsCount = argumentCount(expression)
                if (argumentsCount == functionDescriptor.valueParameters.size)
                    return expression
                val (symbol, params) = parametersForCall(expression)
                val descriptor = symbol.descriptor
                descriptor.typeParameters.forEach { log { "$descriptor [${it.index}]: $it" } }
                descriptor.original.typeParameters.forEach { log { "${descriptor.original}[${it.index}] : $it" } }
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
                        expression.extensionReceiver?.apply {
                            extensionReceiver = expression.extensionReceiver
                        }
                        expression.dispatchReceiver?.apply {
                            dispatchReceiver = expression.dispatchReceiver
                        }
                        log { "call::extension@: ${ir2string(expression.extensionReceiver)}" }
                        log { "call::dispatch@: ${ir2string(expression.dispatchReceiver)}" }
                    }
            }

            private fun IrFunction.findSuperMethodWithDefaultArguments(): IrFunction? {
                if (!this.descriptor.needsDefaultArgumentsLowering(skipInline)) return null

                if (this !is IrSimpleFunction) return this

                this.overriddenSymbols.forEach {
                    it.owner.findSuperMethodWithDefaultArguments()?.let { return it }
                }

                return this
            }

            private fun parametersForCall(expression: IrFunctionAccessExpression): Pair<IrFunctionSymbol, List<Pair<ValueParameterDescriptor, IrExpression?>>> {
                val descriptor = expression.descriptor
                val keyFunction = expression.symbol.owner.findSuperMethodWithDefaultArguments()!!
                val realFunction = keyFunction.generateDefaultsFunction(context)
                realFunction.parent = keyFunction.parent
                val realDescriptor = realFunction.descriptor

                log { "$descriptor -> $realDescriptor" }
                val maskValues = Array((descriptor.valueParameters.size + 31) / 32, { 0 })
                val params = mutableListOf<Pair<ValueParameterDescriptor, IrExpression?>>()
                params.addAll(descriptor.valueParameters.mapIndexed { i, _ ->
                    val valueArgument = expression.getValueArgument(i)
                    if (valueArgument == null) {
                        val maskIndex = i / 32
                        maskValues[maskIndex] = maskValues[maskIndex] or (1 shl (i % 32))
                    }
                    val valueParameterDescriptor = realDescriptor.valueParameters[i]
                    val defaultValueArgument = if (valueParameterDescriptor.isVararg) {
                        null
                    } else {
                        nullConst(expression, realFunction.valueParameters[i].type)
                    }
                    valueParameterDescriptor to (valueArgument ?: defaultValueArgument)
                })
                maskValues.forEachIndexed { i, maskValue ->
                    params += maskParameterDescriptor(realFunction, i) to IrConstImpl.int(
                        startOffset = irBody.startOffset,
                        endOffset = irBody.endOffset,
                        type = context.irBuiltIns.intType,
                        value = maskValue
                    )
                }
                if (expression.descriptor is ClassConstructorDescriptor) {
                    val defaultArgumentMarker = context.ir.symbols.defaultConstructorMarker
                    params += markerParameterDescriptor(realDescriptor) to IrGetObjectValueImpl(
                        startOffset = irBody.startOffset,
                        endOffset = irBody.endOffset,
                        type = defaultArgumentMarker.owner.defaultType,
                        symbol = defaultArgumentMarker
                    )
                } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
                    params += realDescriptor.valueParameters.last() to
                            IrConstImpl.constNull(irBody.startOffset, irBody.endOffset, context.irBuiltIns.nothingNType)
                }
                params.forEach {
                    log { "descriptor::${realDescriptor.name.asString()}#${it.first.index}: ${it.first.name.asString()}" }
                }
                return Pair(realFunction.symbol, params)
            }

            private fun argumentCount(expression: IrMemberAccessExpression) =
                expression.descriptor.valueParameters.count { expression.getValueArgument(it) != null }
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

private fun CallableMemberDescriptor.needsDefaultArgumentsLowering(skipInlineMethods: Boolean) =
    valueParameters.any { it.hasDefaultValue() } && !(this is FunctionDescriptor && isInline && skipInlineMethods)

private fun IrFunction.generateDefaultsFunction(context: CommonBackendContext): IrFunction = with(this.descriptor) {
    return context.ir.defaultParameterDeclarationsCache.getOrPut(this) {
        val descriptor = when (this) {
            is ClassConstructorDescriptor ->
                ClassConstructorDescriptorImpl.create(
                    /* containingDeclaration = */ containingDeclaration,
                    /* annotations           = */ annotations,
                    /* isPrimary             = */ false,
                    /* source                = */ source
                )
            else -> {
                val name = Name.identifier("$name\$default")

                SimpleFunctionDescriptorImpl.create(
                    /* containingDeclaration = */ containingDeclaration,
                    /* annotations           = */ annotations,
                    /* name                  = */ name,
                    /* kind                  = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                    /* source                = */ source
                )
            }
        }

        val function = this@generateDefaultsFunction

        val syntheticParameters = MutableList((valueParameters.size + 31) / 32) { i ->
            valueParameter(descriptor, valueParameters.size + i, parameterMaskName(i), context.irBuiltIns.intType)
        }
        if (this is ClassConstructorDescriptor) {
            syntheticParameters += valueParameter(
                descriptor, syntheticParameters.last().index + 1,
                kConstructorMarkerName,
                context.ir.symbols.defaultConstructorMarker.owner.defaultType
            )
        } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
            syntheticParameters += valueParameter(
                descriptor, syntheticParameters.last().index + 1,
                "handler".synthesizedName,
                context.irBuiltIns.anyType
            )
        }

        val newValueParameters = function.valueParameters.map {
            val parameterDescriptor = ValueParameterDescriptorImpl(
                containingDeclaration = descriptor,
                original = null, /* ValueParameterDescriptorImpl::copy do not save original. */
                index = it.index,
                annotations = it.descriptor.annotations,
                name = it.name,
                outType = it.descriptor.type,
                declaresDefaultValue = false,
                isCrossinline = it.isCrossinline,
                isNoinline = it.isNoinline,
                varargElementType = (it.descriptor as ValueParameterDescriptor).varargElementType,
                source = it.descriptor.source
            )

            it.copy(parameterDescriptor)

        } + syntheticParameters

        descriptor.initialize(
            /* receiverParameterType         = */ extensionReceiverParameter,
            /* dispatchReceiverParameter     = */ dispatchReceiverParameter,
            /* typeParameters                = */ typeParameters.map {
                TypeParameterDescriptorImpl.createForFurtherModification(
                    /* containingDeclaration = */ descriptor,
                    /* annotations           = */ it.annotations,
                    /* reified               = */ it.isReified,
                    /* variance              = */ it.variance,
                    /* name                  = */ it.name,
                    /* index                 = */ it.index,
                    /* source                = */ it.source,
                    /* reportCycleError      = */ null,
                    /* supertypeLoopsChecker = */ SupertypeLoopChecker.EMPTY
                ).apply {
                    it.upperBounds.forEach { addUpperBound(it) }
                    setInitialized()
                }
            },
            /* unsubstitutedValueParameters  = */ newValueParameters.map { it.descriptor as ValueParameterDescriptor },
            /* unsubstitutedReturnType       = */ returnType,
            /* modality                      = */ Modality.FINAL,
            /* visibility                    = */ this.visibility
        )
        descriptor.isSuspend = this.isSuspend
        context.log { "adds to cache[$this] = $descriptor" }

        val startOffset = this.startOffsetOrUndefined
        val endOffset = this.endOffsetOrUndefined

        val result: IrFunction = when (descriptor) {
            is ClassConstructorDescriptor -> IrConstructorImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                descriptor
            )

            else -> IrFunctionImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                descriptor
            )
        }

        result.returnType = function.returnType

        function.typeParameters.mapTo(result.typeParameters) {
            assert(function.descriptor.typeParameters[it.index] == it.descriptor)
            IrTypeParameterImpl(
                startOffset, endOffset, origin, descriptor.typeParameters[it.index]
            ).apply { this.superTypes += it.superTypes }
        }
        result.parent = function.parent
        result.createDispatchReceiverParameter()

        function.extensionReceiverParameter?.let {
            result.extensionReceiverParameter = IrValueParameterImpl(
                it.startOffset,
                it.endOffset,
                it.origin,
                descriptor.extensionReceiverParameter!!,
                it.type,
                it.varargElementType
            ).apply { parent = result }
        }

        result.valueParameters += newValueParameters.also { it.forEach { it.parent = result } }

        function.annotations.mapTo(result.annotations) { it.deepCopyWithSymbols() }

        result
    }
}

object DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER :
    IrDeclarationOriginImpl("DEFAULT_PARAMETER_EXTENT")

private fun IrFunction.valueParameter(descriptor: FunctionDescriptor, index: Int, name: Name, type: IrType): IrValueParameter {
    val parameterDescriptor = ValueParameterDescriptorImpl(
        containingDeclaration = descriptor,
        original = null,
        index = index,
        annotations = Annotations.EMPTY,
        name = name,
        outType = type.toKotlinType(),
        declaresDefaultValue = false,
        isCrossinline = false,
        isNoinline = false,
        varargElementType = null,
        source = SourceElement.NO_SOURCE
    )
    return IrValueParameterImpl(
        startOffset,
        endOffset,
        IrDeclarationOrigin.DEFINED,
        parameterDescriptor,
        type,
        null
    )
}

internal val kConstructorMarkerName = "marker".synthesizedName

private fun parameterMaskName(number: Int) = "mask$number".synthesizedName
