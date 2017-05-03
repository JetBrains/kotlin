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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.backend.konan.descriptors.konanInternal
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.ir2string
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.util.OperatorNameConventions

class DefaultArgumentStubGenerator internal constructor(val context: Context): DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction)
                lower(memberDeclaration)
            else
                null
        }

    }

    object DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER :
            IrDeclarationOriginImpl("DEFAULT_PARAMETER_EXTENT")

    private fun lower(irFunction: IrFunction): List<IrFunction> {
        val functionDescriptor = irFunction.descriptor

        if (!functionDescriptor.needsDefaultArgumentsLowering)
            return listOf(irFunction)

        val bodies = functionDescriptor.valueParameters
                .mapNotNull{irFunction.getDefault(it)}


        log("detected ${functionDescriptor.name.asString()} has got #${bodies.size} default expressions")
        functionDescriptor.overriddenDescriptors.forEach { context.log{"DEFAULT-REPLACER: $it"} }
        if (bodies.isNotEmpty()) {
            val descriptor = functionDescriptor.generateDefaultsDescription(context)
            log("$functionDescriptor -> $descriptor")
            val builder = context.createIrBuilder(descriptor)
            val body = builder.irBlockBody(irFunction) {
                val params = mutableListOf<VariableDescriptor>()
                val variables = mutableMapOf<ValueDescriptor, ValueDescriptor>()
                if (descriptor.extensionReceiverParameter != null) {
                    variables[functionDescriptor.extensionReceiverParameter!!] = descriptor.extensionReceiverParameter!!
                }

                for (valueParameter in functionDescriptor.valueParameters) {
                    val parameterDescriptor = descriptor.valueParameters[valueParameter.index]
                    val temporaryVariableDescriptor = scope.createTemporaryVariableDescriptor(parameterDescriptor)
                    params.add(temporaryVariableDescriptor)
                    variables.put(valueParameter, temporaryVariableDescriptor)
                    if (valueParameter.hasDefaultValue()) {
                        val kIntAnd = valueParameter.builtIns.intType.memberScope
                                .getContributedFunctions(OperatorNameConventions.AND, NoLookupLocation.FROM_BACKEND)
                                .single()
                        val condition = irNotEquals(irCall(kIntAnd).apply {
                            dispatchReceiver = irGet(maskParameterDescriptor(descriptor, valueParameter.index / 32))
                            putValueArgument(0, irInt(1 shl (valueParameter.index % 32)))
                        }, irInt(0))
                        val expressionBody = getDefaultParameterExpressionBody(irFunction, valueParameter)

                        /* Use previously calculated values in next expression. */
                        expressionBody.transformChildrenVoid(object:IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                log("GetValue: ${expression.descriptor}")
                                val valueDescriptor = variables[expression.descriptor]
                                return when (valueDescriptor) {
                                    is VariableDescriptor          -> irGet(valueDescriptor)
                                    is ReceiverParameterDescriptor -> irGet(valueDescriptor)
                                    null                           -> expression
                                    else                           -> TODO("$valueDescriptor")
                                }

                            }
                        })
                        val variableInitialization = irIfThenElse(
                                type      = temporaryVariableDescriptor.type,
                                condition = condition,
                                thenPart  = expressionBody.expression,
                                elsePart  = irGet(parameterDescriptor))
                        + scope.createTemporaryVariable(
                                descriptor  = temporaryVariableDescriptor,
                                initializer = variableInitialization)
                        /* Mapping calculated values with its origin variables. */
                    } else {
                        + scope.createTemporaryVariable(
                                descriptor   = temporaryVariableDescriptor,
                                initializer = irGet(parameterDescriptor))
                    }
                }
                if (functionDescriptor is ClassConstructorDescriptor) {
                    + IrDelegatingConstructorCallImpl(
                            startOffset = irFunction.startOffset,
                            endOffset   = irFunction.endOffset,
                            constructorDescriptor = functionDescriptor
                    ).apply {
                        params.forEachIndexed { i, variable ->
                            putValueArgument(i, irGet(variable))
                        }
                        if (functionDescriptor.dispatchReceiverParameter != null) {
                            dispatchReceiver = irThis()
                        }
                    }
                } else {
                    +irReturn(irCall(functionDescriptor).apply {
                        if (functionDescriptor.dispatchReceiverParameter != null) {
                            dispatchReceiver = irThis()
                        }
                        if (functionDescriptor.extensionReceiverParameter != null) {
                            extensionReceiver = irGet(variables[functionDescriptor.extensionReceiverParameter!!] as ReceiverParameterDescriptor)
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


    private fun log(msg:String) = context.log{"DEFAULT-REPLACER: $msg"}
}

private fun Scope.createTemporaryVariableDescriptor(parameterDescriptor: ValueParameterDescriptor?): VariableDescriptor =
        IrTemporaryVariableDescriptorImpl(
                containingDeclaration = this.scopeOwner,
                name                  = parameterDescriptor!!.name.asString().synthesizedName,
                outType               = parameterDescriptor.type,
                isMutable             = false)

private fun Scope.createTemporaryVariable(descriptor: VariableDescriptor, initializer: IrExpression) =
        IrVariableImpl(
                startOffset = initializer.startOffset,
                endOffset   = initializer.endOffset,
                origin      = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                descriptor  = descriptor,
                initializer = initializer)

private fun getDefaultParameterExpressionBody(irFunction: IrFunction, valueParameter: ValueParameterDescriptor):IrExpressionBody {
    return irFunction.getDefault(valueParameter) as? IrExpressionBody ?: TODO("FIXME!!!")
}

private fun maskParameterDescriptor(descriptor: FunctionDescriptor, number:Int)= descriptor.valueParameters.single { it.name == parameterMaskName(number) }

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

class DefaultParameterInjector internal constructor(val context: Context): BodyLoweringPass {
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
                val (descriptorForCall, params) = parametersForCall(expression)
                return IrDelegatingConstructorCallImpl(
                        startOffset = expression.startOffset,
                        endOffset   = expression.endOffset,
                        constructorDescriptor = descriptorForCall as ClassConstructorDescriptor)
                            .apply {
                                params.forEach {
                                    log("call::params@${it.first.index}/${it.first.name.asString()}: ${ir2string(it.second)}")
                                    putValueArgument(it.first.index, it.second)
                                }
                                dispatchReceiver = expression.dispatchReceiver
                            }

            }

            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val functionDescriptor = expression.descriptor as FunctionDescriptor

                if (!functionDescriptor.needsDefaultArgumentsLowering)
                    return expression

                val argumentsCount = argumentCount(expression)
                if (argumentsCount == functionDescriptor.valueParameters.size)
                    return expression
                val (descriptor, params) = parametersForCall(expression)
                descriptor.typeParameters.forEach { log("$descriptor [${it.index}]: $it") }
                descriptor.original.typeParameters.forEach { log("${descriptor.original}[${it.index}] : $it") }
                return IrCallImpl(
                        startOffset   = expression.startOffset,
                        endOffset     = expression.endOffset,
                        calleeDescriptor = descriptor,
                        typeArguments = expression.descriptor.typeParameters.map{it to (expression.getTypeArgument(it) ?: it.defaultType) }.toMap())
                        .apply {
                            params.forEach {
                                log("call::params@${it.first.index}/${it.first.name.asString()}: ${ir2string(it.second)}")
                                putValueArgument(it.first.index, it.second)
                            }
                            expression.extensionReceiver?.apply{
                                extensionReceiver = expression.extensionReceiver
                            }
                            expression.dispatchReceiver?.apply {
                                dispatchReceiver = expression.dispatchReceiver
                            }
                            log("call::extension@: ${ir2string(expression.extensionReceiver)}")
                            log("call::dispatch@: ${ir2string(expression.dispatchReceiver)}")
                        }
            }

            private fun parametersForCall(expression: IrMemberAccessExpression): Pair<FunctionDescriptor, List<Pair<ValueParameterDescriptor, IrExpression?>>> {
                val descriptor = expression.descriptor as FunctionDescriptor
                val keyDescriptor = if (DescriptorUtils.isOverride(descriptor))
                    DescriptorUtils.getAllOverriddenDescriptors(descriptor).first()
                else
                    descriptor.original
                val realDescriptor = keyDescriptor.generateDefaultsDescription(context)

                log("$descriptor -> $realDescriptor")
                val maskValues = Array(descriptor.valueParameters.size / 32 + 1, {0})
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
                    params += maskParameterDescriptor(realDescriptor, i) to IrConstImpl.int(
                            startOffset = irBody.startOffset,
                            endOffset   = irBody.endOffset,
                            type        = descriptor.builtIns.intType,
                            value       = maskValue)
                }
                if (expression.descriptor is ClassConstructorDescriptor) {
                    val defaultArgumentMarker = getDefaultArgumentMarkerClassDescriptor(context.builtIns)
                    params += markerParameterDescriptor(realDescriptor) to IrGetObjectValueImpl(
                            startOffset = irBody.startOffset,
                            endOffset   = irBody.endOffset,
                            type        = defaultArgumentMarker.defaultType,
                            descriptor  = defaultArgumentMarker)
                }
                params.forEach {
                    log("descriptor::${realDescriptor.name.asString()}#${it.first.index}: ${it.first.name.asString()}")
                }
                return Pair(realDescriptor, params)
            }

            private fun argumentCount(expression: IrMemberAccessExpression) =
                expression.descriptor.valueParameters.count { expression.getValueArgument(it) != null }
        })
    }

    private fun log(msg: String) = context.log{"DEFAULT-INJECTOR: $msg"}
}

private val CallableMemberDescriptor.needsDefaultArgumentsLowering
    get() = valueParameters.any { it.hasDefaultValue() } && !(this is FunctionDescriptor && isInline)

private fun FunctionDescriptor.generateDefaultsDescription(context: Context): FunctionDescriptor {
    return context.ir.defaultParameterDescriptorsCache.getOrPut(this) {
        val descriptor = when (this) {
            is ClassConstructorDescriptor ->
                ClassConstructorDescriptorImpl.create(
                        /* containingDeclaration = */ containingDeclaration,
                        /* annotations           = */ annotations,
                        /* isPrimary             = */ false,
                        /* source                = */ SourceElement.NO_SOURCE)
            is FunctionDescriptor -> {
                val name = Name.identifier("$name\$default")

                SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ containingDeclaration,
                        /* annotations           = */ annotations,
                        /* name                  = */ name,
                        /* kind                  = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                        /* source                = */ SourceElement.NO_SOURCE)
            }
            else -> TODO("FIXME: $this")
        }

        val syntheticParameters = MutableList(valueParameters.size / 32 + 1) { i ->
            valueParameter(descriptor, valueParameters.size + i, parameterMaskName(i), descriptor.builtIns.intType)
        }
        if (this is ClassConstructorDescriptor) {
            syntheticParameters += valueParameter(descriptor, syntheticParameters.last().index + 1,
                    kConstructorMarkerName,
                    getDefaultArgumentMarkerClassDescriptor(context.builtIns).defaultType)
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
        descriptor
    }
}

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

private fun getDefaultArgumentMarkerClassDescriptor(builtIns: KonanBuiltIns): ClassDescriptor =
        builtIns.konanInternal.getContributedClassifier(
                Name.identifier("DefaultArgumentMarker"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

internal fun IrBuilderWithScope.irGet(descriptor: ReceiverParameterDescriptor):IrGetValue = IrGetValueImpl(startOffset, endOffset, descriptor)
private fun parameterMaskName(number: Int) = "mask$number".synthesizedName
