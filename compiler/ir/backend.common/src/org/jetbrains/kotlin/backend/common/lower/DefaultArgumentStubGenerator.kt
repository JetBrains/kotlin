/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
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
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns

open class DefaultArgumentStubGenerator constructor(val context: CommonBackendContext): DeclarationContainerLoweringPass {
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

        if (!functionDescriptor.needsDefaultArgumentsLowering)
            return listOf(irFunction)

        val bodies = functionDescriptor.valueParameters
                .mapNotNull{irFunction.getDefault(it)}


        log { "detected ${functionDescriptor.name.asString()} has got #${bodies.size} default expressions" }
        functionDescriptor.overriddenDescriptors.forEach { context.log{"DEFAULT-REPLACER: $it"} }
        if (bodies.isNotEmpty()) {
            val newIrFunction = functionDescriptor.generateDefaultsFunction(context)
            val descriptor = newIrFunction.descriptor
            log { "$functionDescriptor -> $descriptor" }
            val builder = context.createIrBuilder(newIrFunction.symbol)
            val body = builder.irBlockBody(irFunction) {
                val params = mutableListOf<IrVariableSymbol>()
                val variables = mutableMapOf<ValueDescriptor, IrValueSymbol>()
                if (descriptor.extensionReceiverParameter != null) {
                    variables[functionDescriptor.extensionReceiverParameter!!] =
                            newIrFunction.extensionReceiverParameter!!.symbol
                }

                for (valueParameter in functionDescriptor.valueParameters) {
                    val parameterSymbol = newIrFunction.valueParameters[valueParameter.index].symbol
                    val temporaryVariableSymbol =
                            IrVariableSymbolImpl(scope.createTemporaryVariableDescriptor(parameterSymbol.descriptor))
                    params.add(temporaryVariableSymbol)
                    variables.put(valueParameter, temporaryVariableSymbol)
                    if (valueParameter.hasDefaultValue()) {
                        val kIntAnd = symbols.intAnd
                        val condition = irNotEquals(irCall(kIntAnd).apply {
                            dispatchReceiver = irGet(maskParameterSymbol(newIrFunction, valueParameter.index / 32))
                            putValueArgument(0, irInt(1 shl (valueParameter.index % 32)))
                        }, irInt(0))
                        val expressionBody = getDefaultParameterExpressionBody(irFunction, valueParameter)

                        /* Use previously calculated values in next expression. */
                        expressionBody.transformChildrenVoid(object:IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                log { "GetValue: ${expression.descriptor}" }
                                val valueSymbol = variables[expression.descriptor] ?: return expression
                                return irGet(valueSymbol)
                            }
                        })
                        val variableInitialization = irIfThenElse(
                                type      = temporaryVariableSymbol.descriptor.type,
                                condition = condition,
                                thenPart  = expressionBody.expression,
                                elsePart  = irGet(parameterSymbol))
                        + scope.createTemporaryVariable(
                                symbol  = temporaryVariableSymbol,
                                initializer = variableInitialization)
                        /* Mapping calculated values with its origin variables. */
                    } else {
                        + scope.createTemporaryVariable(
                                symbol  = temporaryVariableSymbol,
                                initializer = irGet(parameterSymbol))
                    }
                }
                if (irFunction is IrConstructor) {
                    + IrDelegatingConstructorCallImpl(
                            startOffset = irFunction.startOffset,
                            endOffset   = irFunction.endOffset,
                            symbol = irFunction.symbol, descriptor = irFunction.symbol.descriptor
                    ).apply {
                        params.forEachIndexed { i, variable ->
                            putValueArgument(i, irGet(variable))
                        }
                        if (functionDescriptor.dispatchReceiverParameter != null) {
                            dispatchReceiver = irGet(newIrFunction.dispatchReceiverParameter!!.symbol)
                        }
                    }
                } else {
                    +irReturn(irCall(irFunction.symbol).apply {
                        if (functionDescriptor.dispatchReceiverParameter != null) {
                            dispatchReceiver = irGet(newIrFunction.dispatchReceiverParameter!!.symbol)
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
            return if (functionDescriptor is ClassConstructorDescriptor)
                listOf(irFunction, IrConstructorImpl(
                        startOffset = irFunction.startOffset,
                        endOffset   = irFunction.endOffset,
                        descriptor  = descriptor as ClassConstructorDescriptor,
                        origin      = DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                        body        = body).apply { createParameterDeclarations() })
            else
                listOf(irFunction, IrFunctionImpl(
                        startOffset = irFunction.startOffset,
                        endOffset   = irFunction.endOffset,
                        descriptor  = descriptor,
                        origin      = DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                        body        = body).apply { createParameterDeclarations() })
        }
        return listOf(irFunction)
    }


    private fun log(msg: () -> String) = context.log { "DEFAULT-REPLACER: ${msg()}" }
}

private fun Scope.createTemporaryVariableDescriptor(parameterDescriptor: ParameterDescriptor?): VariableDescriptor =
        IrTemporaryVariableDescriptorImpl(
                containingDeclaration = this.scopeOwner,
                name                  = parameterDescriptor!!.name.asString().synthesizedName,
                outType               = parameterDescriptor.type,
                isMutable             = false)

private fun Scope.createTemporaryVariable(symbol: IrVariableSymbol, initializer: IrExpression) =
        IrVariableImpl(
                startOffset = initializer.startOffset,
                endOffset   = initializer.endOffset,
                origin      = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                symbol      = symbol).apply {

            this.initializer = initializer
        }

private fun getDefaultParameterExpressionBody(irFunction: IrFunction, valueParameter: ValueParameterDescriptor):IrExpressionBody {
    return irFunction.getDefault(valueParameter) ?: TODO("FIXME!!!")
}

private fun maskParameterDescriptor(function: IrFunction, number: Int) =
        maskParameterSymbol(function, number).descriptor as ValueParameterDescriptor
private fun maskParameterSymbol(function: IrFunction, number: Int) =
        function.valueParameters.single { it.descriptor.name == parameterMaskName(number) }.symbol

private fun markerParameterDescriptor(descriptor: FunctionDescriptor) = descriptor.valueParameters.single { it.name == kConstructorMarkerName }

private fun nullConst(expression: IrElement, type: KotlinType): IrExpression? {
    when {
        KotlinBuiltIns.isFloat(type)   -> return IrConstImpl.float     (expression.startOffset, expression.endOffset, type, 0.0F)
        KotlinBuiltIns.isDouble(type)  -> return IrConstImpl.double    (expression.startOffset, expression.endOffset, type, 0.0)
        KotlinBuiltIns.isBoolean(type) -> return IrConstImpl.boolean   (expression.startOffset, expression.endOffset, type, false)
        KotlinBuiltIns.isByte(type)    -> return IrConstImpl.byte      (expression.startOffset, expression.endOffset, type, 0)
        KotlinBuiltIns.isChar(type)    -> return IrConstImpl.char      (expression.startOffset, expression.endOffset, type, 0.toChar())
        KotlinBuiltIns.isShort(type)   -> return IrConstImpl.short     (expression.startOffset, expression.endOffset, type, 0)
        KotlinBuiltIns.isInt(type)     -> return IrConstImpl.int       (expression.startOffset, expression.endOffset, type, 0)
        KotlinBuiltIns.isLong(type)    -> return IrConstImpl.long      (expression.startOffset, expression.endOffset, type, 0)
        else                           -> return IrConstImpl.constNull (expression.startOffset, expression.endOffset, type.builtIns.nullableNothingType)
    }
}

class DefaultParameterInjector constructor(val context: CommonBackendContext): BodyLoweringPass {
    override fun lower(irBody: IrBody) {

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                super.visitDelegatingConstructorCall(expression)
                val descriptor = expression.descriptor
                if (!descriptor.needsDefaultArgumentsLowering)
                    return expression
                val argumentsCount = argumentCount(expression)
                if (argumentsCount == descriptor.valueParameters.size)
                    return expression
                val (symbolForCall, params) = parametersForCall(expression)
                return IrDelegatingConstructorCallImpl(
                        startOffset = expression.startOffset,
                        endOffset   = expression.endOffset,
                        symbol      = symbolForCall as IrConstructorSymbol,
                        descriptor  = symbolForCall.descriptor)
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

                if (!functionDescriptor.needsDefaultArgumentsLowering)
                    return expression

                val argumentsCount = argumentCount(expression)
                if (argumentsCount == functionDescriptor.valueParameters.size)
                    return expression
                val (symbol, params) = parametersForCall(expression)
                val descriptor = symbol.descriptor
                descriptor.typeParameters.forEach { log { "$descriptor [${it.index}]: $it" } }
                descriptor.original.typeParameters.forEach { log { "${descriptor.original}[${it.index}] : $it" } }
                return IrCallImpl(
                        startOffset   = expression.startOffset,
                        endOffset     = expression.endOffset,
                        symbol        = symbol,
                        descriptor    = descriptor,
                        typeArguments = expression.descriptor.typeParameters.map{it to (expression.getTypeArgument(it) ?: it.defaultType) }.toMap())
                        .apply {
                            params.forEach {
                                log { "call::params@${it.first.index}/${it.first.name.asString()}: ${ir2string(it.second)}" }
                                putValueArgument(it.first.index, it.second)
                            }
                            expression.extensionReceiver?.apply{
                                extensionReceiver = expression.extensionReceiver
                            }
                            expression.dispatchReceiver?.apply {
                                dispatchReceiver = expression.dispatchReceiver
                            }
                            log { "call::extension@: ${ir2string(expression.extensionReceiver)}" }
                            log { "call::dispatch@: ${ir2string(expression.dispatchReceiver)}" }
                        }
            }

            private fun parametersForCall(expression: IrMemberAccessExpression): Pair<IrFunctionSymbol, List<Pair<ValueParameterDescriptor, IrExpression?>>> {
                val descriptor = expression.descriptor as FunctionDescriptor
                val keyDescriptor = if (DescriptorUtils.isOverride(descriptor))
                    DescriptorUtils.getAllOverriddenDescriptors(descriptor).first()
                else
                    descriptor.original
                val realFunction = keyDescriptor.generateDefaultsFunction(context)
                val realDescriptor = realFunction.descriptor

                log { "$descriptor -> $realDescriptor" }
                val maskValues = Array((descriptor.valueParameters.size + 31) / 32, {0})
                val params = mutableListOf<Pair<ValueParameterDescriptor, IrExpression?>>()
                params.addAll(descriptor.valueParameters.mapIndexed { i, _ ->
                    val valueArgument = expression.getValueArgument(i)
                    if (valueArgument == null) {
                        val maskIndex = i / 32
                        maskValues[maskIndex] = maskValues[maskIndex] or (1 shl (i % 32))
                    }
                    val valueParameterDescriptor = realDescriptor.valueParameters[i]
                    val pair = valueParameterDescriptor to (valueArgument ?: nullConst(expression, valueParameterDescriptor.type))
                    return@mapIndexed pair
                })
                maskValues.forEachIndexed { i, maskValue ->
                    params += maskParameterDescriptor(realFunction, i) to IrConstImpl.int(
                            startOffset = irBody.startOffset,
                            endOffset   = irBody.endOffset,
                            type        = descriptor.builtIns.intType,
                            value       = maskValue)
                }
                if (expression.descriptor is ClassConstructorDescriptor) {
                    val defaultArgumentMarker = context.ir.symbols.defaultConstructorMarker
                    params += markerParameterDescriptor(realDescriptor) to IrGetObjectValueImpl(
                            startOffset = irBody.startOffset,
                            endOffset   = irBody.endOffset,
                            type        = defaultArgumentMarker.owner.defaultType,
                            symbol      = defaultArgumentMarker)
                }
                else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
                    params += realDescriptor.valueParameters.last() to
                            IrConstImpl.constNull(irBody.startOffset, irBody.endOffset, context.builtIns.any.defaultType)
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

    private fun log(msg: () -> String) = context.log { "DEFAULT-INJECTOR: ${msg()}" }
}

private val CallableMemberDescriptor.needsDefaultArgumentsLowering
    get() = valueParameters.any { it.hasDefaultValue() } && !(this is FunctionDescriptor && isInline)

private fun FunctionDescriptor.generateDefaultsFunction(context: CommonBackendContext): IrFunction {
    return context.ir.defaultParameterDeclarationsCache.getOrPut(this) {
        val descriptor = when (this) {
            is ClassConstructorDescriptor ->
                ClassConstructorDescriptorImpl.create(
                        /* containingDeclaration = */ containingDeclaration,
                        /* annotations           = */ annotations,
                        /* isPrimary             = */ false,
                        /* source                = */ source)
            else -> {
                val name = Name.identifier("$name\$default")

                SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ containingDeclaration,
                        /* annotations           = */ annotations,
                        /* name                  = */ name,
                        /* kind                  = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                        /* source                = */ source)
            }
        }

        val syntheticParameters = MutableList((valueParameters.size + 31) / 32) { i ->
            valueParameter(descriptor, valueParameters.size + i, parameterMaskName(i), descriptor.builtIns.intType)
        }
        if (this is ClassConstructorDescriptor) {
            syntheticParameters += valueParameter(descriptor, syntheticParameters.last().index + 1,
                                                  kConstructorMarkerName,
                                                  context.ir.symbols.defaultConstructorMarker.owner.defaultType)
        }
        else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
            syntheticParameters += valueParameter(descriptor, syntheticParameters.last().index + 1,
                                                  "handler".synthesizedName,
                                                  context.ir.symbols.any.owner.defaultType)
        }

        descriptor.initialize(
                /* receiverParameterType         = */ extensionReceiverParameter?.type,
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
                /* unsubstitutedValueParameters  = */ valueParameters.map {
            ValueParameterDescriptorImpl(
                    containingDeclaration = descriptor,
                    original              = null, /* ValueParameterDescriptorImpl::copy do not save original. */
                    index                 = it.index,
                    annotations           = it.annotations,
                    name                  = it.name,
                    outType               = it.type,
                    declaresDefaultValue  = false,
                    isCrossinline         = it.isCrossinline,
                    isNoinline            = it.isNoinline,
                    varargElementType     = it.varargElementType,
                    source                = it.source)
        } + syntheticParameters,
                /* unsubstitutedReturnType       = */ returnType,
                /* modality                      = */ Modality.FINAL,
                /* visibility                    = */ this.visibility)
        descriptor.isSuspend = this.isSuspend
        context.log{"adds to cache[$this] = $descriptor"}

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

        result.createParameterDeclarations()

        result
    }
}

object DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER :
        IrDeclarationOriginImpl("DEFAULT_PARAMETER_EXTENT")

private fun valueParameter(descriptor: FunctionDescriptor, index: Int, name: Name, type: KotlinType):ValueParameterDescriptor {
    return ValueParameterDescriptorImpl(
            containingDeclaration = descriptor,
            original              = null,
            index                 = index,
            annotations           = Annotations.EMPTY,
            name                  = name,
            outType               = type,
            declaresDefaultValue  = false,
            isCrossinline         = false,
            isNoinline            = false,
            varargElementType     = null,
            source                = SourceElement.NO_SOURCE
    )
}

internal val kConstructorMarkerName = "marker".synthesizedName

private fun parameterMaskName(number: Int) = "mask$number".synthesizedName
