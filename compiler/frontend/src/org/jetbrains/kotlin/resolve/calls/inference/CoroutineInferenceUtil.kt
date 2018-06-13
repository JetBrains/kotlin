/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.coroutines.hasSuspendFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver.getCallableReferenceExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver.getFunctionLiteralArgumentIfAny
import org.jetbrains.kotlin.resolve.calls.CallCompleter
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.hasUnknownFunctionParameter
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.checker.TypeCheckerContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.contains
import javax.inject.Inject

class TypeTemplate(
    val typeVariable: TypeVariable,
    val coroutineInferenceData: CoroutineInferenceData,
    nullable: Boolean = true
) : FlexibleType(
    typeVariable.originalTypeParameter.builtIns.nothingType,
    typeVariable.originalTypeParameter.builtIns.anyType.makeNullableAsSpecified(nullable)
) {
    override fun replaceAnnotations(newAnnotations: Annotations) = this

    override fun makeNullableAsSpecified(newNullability: Boolean) = TypeTemplate(typeVariable, coroutineInferenceData, newNullability)

    override val delegate: SimpleType
        get() = upperBound

    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions) =
        "~${renderer.renderType(typeVariable.type)}"
}

class CoroutineInferenceData {
    private val csBuilder = ConstraintSystemBuilderImpl()
    private val typeTemplates = HashMap<TypeVariable, TypeTemplate>()
    private var hereIsBadCall = false

    fun getTypeTemplate(typeVariable: TypeVariable) =
        typeTemplates.getOrPut(typeVariable) {
            TypeTemplate(typeVariable, this)
        }

    fun initSystem() {
        csBuilder.registerTypeVariables(CallHandle.NONE, typeTemplates.keys.map { it.freshTypeParameter })
    }

    fun toNewVariableType(type: KotlinType): KotlinType {
        return (type.unwrap() as? TypeTemplate)?.typeVariable?.freshTypeParameter?.let { typeVariable ->
            csBuilder.typeVariableSubstitutors[CallHandle.NONE]?.substitution?.get(typeVariable.defaultType)?.type
        } ?: type
    }

    fun addConstraint(subType: KotlinType, superType: KotlinType) {
        csBuilder.addSubtypeConstraint(toNewVariableType(subType), toNewVariableType(superType), ConstraintPositionKind.SPECIAL.position())
    }

    fun reportInferenceResult(externalCSBuilder: ConstraintSystem.Builder) {
        if (hereIsBadCall) return

        val resultingSubstitution = csBuilder.build().resultingSubstitutor.substitution
        for ((originalTypeVariable) in typeTemplates) {
            resultingSubstitution[originalTypeVariable.type]?.type.let {
                externalCSBuilder.addSubtypeConstraint(originalTypeVariable.type, it, ConstraintPositionKind.FROM_COMPLETER.position())
                externalCSBuilder.addSubtypeConstraint(it, originalTypeVariable.type, ConstraintPositionKind.FROM_COMPLETER.position())
            }
        }
    }

    fun badCallHappened() {
        hereIsBadCall = true
    }
}

class CoroutineInferenceSupport(
    val argumentTypeResolver: ArgumentTypeResolver,
    val expressionTypingServices: ExpressionTypingServices
) {
    @set:Inject
    lateinit var callCompleter: CallCompleter

    fun analyzeCoroutine(
        functionLiteral: KtFunction,
        valueArgument: ValueArgument,
        csBuilder: ConstraintSystem.Builder,
        context: CallCandidateResolutionContext<*>,
        lambdaExpectedType: KotlinType
    ) {
        val argumentExpression = valueArgument.getArgumentExpression() ?: return
        if (!lambdaExpectedType.isSuspendFunctionType) return
        val lambdaReceiverType = lambdaExpectedType.getReceiverTypeFromFunctionType() ?: return

        val inferenceData = CoroutineInferenceData()

        val constraintSystem = csBuilder.build()
        val newSubstitution = object : DelegatedTypeSubstitution(constraintSystem.currentSubstitutor.substitution) {
            override fun get(key: KotlinType): TypeProjection? {
                val substitutedType = super.get(key)
                if (substitutedType?.type != TypeUtils.DONT_CARE) return substitutedType

                // todo: what about nullable type?
                val typeVariable = constraintSystem.typeVariables.firstOrNull {
                    it.originalTypeParameter.defaultType == key
                } ?: return substitutedType

                return inferenceData.getTypeTemplate(typeVariable).asTypeProjection()
            }

            override fun approximateContravariantCapturedTypes() = true
        }
        val newReceiverType = newSubstitution.buildSubstitutor().substitute(lambdaReceiverType, Variance.INVARIANT) ?: return

        val approximationSubstitutor = object : DelegatedTypeSubstitution(constraintSystem.currentSubstitutor.substitution) {
            override fun approximateContravariantCapturedTypes() = true
        }
        val approximatedLambdaType =
            approximationSubstitutor.buildSubstitutor().substitute(lambdaExpectedType, Variance.IN_VARIANCE) ?: return

        val newExpectedType = createFunctionType(
            newReceiverType.builtIns, approximatedLambdaType.annotations, newReceiverType,
            approximatedLambdaType.getValueParameterTypesFromFunctionType().map(TypeProjection::getType),
            parameterNames = null, // TODO: parameterNames
            returnType = approximatedLambdaType.getReturnTypeFromFunctionType(),
            suspendFunction = true
        )

        if (hasUnknownFunctionParameter(newExpectedType)) return

        inferenceData.initSystem()

        // this trace shouldn't be committed
        val temporaryForCoroutine = TemporaryTraceAndCache.create(
            context, "trace for type argument inference for coroutine", functionLiteral
        )

        val newContext = context.replaceExpectedType(newExpectedType)
            .replaceDataFlowInfo(context.candidateCall.dataFlowInfoForArguments.getInfo(valueArgument))
            .replaceContextDependency(ContextDependency.INDEPENDENT).replaceTraceAndCache(temporaryForCoroutine)
        argumentTypeResolver.getFunctionLiteralTypeInfo(argumentExpression, functionLiteral, newContext, RESOLVE_FUNCTION_ARGUMENTS, true)

        inferenceData.reportInferenceResult(csBuilder)
    }

    fun checkCoroutineCalls(
        context: BasicCallResolutionContext,
        tracingStrategy: TracingStrategy,
        overloadResults: OverloadResolutionResultsImpl<*>
    ) {
        val inferenceData = overloadResults.getCoroutineInferenceData() ?: return

        val resultingCall = overloadResults.resultingCall

        forceInferenceForArguments(context) { _: ValueArgument, _: KotlinType -> /* do nothing */ }

        callCompleter.completeCall(context, overloadResults, tracingStrategy)
        if (!resultingCall.isReallySuccess()) return

        if (isBadCall(resultingCall.resultingDescriptor)) {
            inferenceData.badCallHappened()
        }

        forceInferenceForArguments(context) { valueArgument: ValueArgument, kotlinType: KotlinType ->
            val argumentMatch = resultingCall.getArgumentMapping(valueArgument) as? ArgumentMatch
                    ?: return@forceInferenceForArguments

            with(NewKotlinTypeChecker) {
                val parameterType = getEffectiveExpectedType(argumentMatch.valueParameter, valueArgument, context)
                CoroutineTypeCheckerContext().isSubtypeOf(kotlinType.unwrap(), parameterType.unwrap())
            }
        }

        val extensionReceiver = resultingCall.resultingDescriptor.extensionReceiverParameter ?: return
        resultingCall.extensionReceiver?.let { actualReceiver ->
            with(NewKotlinTypeChecker) {
                CoroutineTypeCheckerContext().isSubtypeOf(actualReceiver.type.unwrap(), extensionReceiver.value.type.unwrap())
            }
        }
    }

    private fun isBadCall(resultingDescriptor: CallableDescriptor): Boolean {
        fun KotlinType.containsTypeTemplate() = contains { it is TypeTemplate }

        val returnType = resultingDescriptor.returnType ?: return true
        if (returnType.containsTypeTemplate()) return true

        if (resultingDescriptor !is FunctionDescriptor || resultingDescriptor.isSuspend) return false

        for (valueParameter in resultingDescriptor.valueParameters) {
            if (valueParameter.type.containsTypeTemplate()) return true
        }
        return false
    }

    private class CoroutineTypeCheckerContext : TypeCheckerContext(errorTypeEqualsToAnything = true) {
        override fun addSubtypeConstraint(subType: UnwrappedType, superType: UnwrappedType): Boolean? {
            (subType as? TypeTemplate ?: superType as? TypeTemplate)?.coroutineInferenceData?.addConstraint(subType, superType)
            return null
        }
    }

    private fun forceInferenceForArguments(
        context: CallResolutionContext<*>,
        callback: (argument: ValueArgument, argumentType: KotlinType) -> Unit
    ) {
        val infoForArguments = context.dataFlowInfoForArguments
        val call = context.call
        val baseContext = context.replaceContextDependency(ContextDependency.INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE)

        for (argument in call.valueArguments) {
            val expression = argument.getArgumentExpression() ?: continue
            val typeInfoForCall = getArgumentTypeInfo(expression, baseContext.replaceDataFlowInfo(infoForArguments.getInfo(argument)))
            typeInfoForCall.type?.let { callback(argument, it) }
        }
    }

    private fun getArgumentTypeInfo(
        expression: KtExpression,
        context: CallResolutionContext<*>
    ): KotlinTypeInfo {
        getFunctionLiteralArgumentIfAny(expression, context)?.let {
            return argumentTypeResolver.getFunctionLiteralTypeInfo(expression, it, context, RESOLVE_FUNCTION_ARGUMENTS, false)
        }

        getCallableReferenceExpressionIfAny(expression, context)?.let {
            return argumentTypeResolver.getCallableReferenceTypeInfo(expression, it, context, RESOLVE_FUNCTION_ARGUMENTS)
        }

        return expressionTypingServices.getTypeInfo(expression, context)
    }
}

fun isCoroutineCallWithAdditionalInference(parameterDescriptor: ValueParameterDescriptor, argument: ValueArgument) =
    parameterDescriptor.hasSuspendFunctionType &&
            argument.getArgumentExpression() is KtLambdaExpression &&
            parameterDescriptor.type.let { it.isBuiltinFunctionalType && it.getReceiverTypeFromFunctionType() != null }


fun OverloadResolutionResultsImpl<*>.isResultWithCoroutineInference() = getCoroutineInferenceData() != null

private fun OverloadResolutionResultsImpl<*>.getCoroutineInferenceData(): CoroutineInferenceData? {
    if (!isSingleResult) return null

    fun getData(receiverValue: ReceiverValue?): CoroutineInferenceData? {
        var coroutineInferenceData: CoroutineInferenceData? = null
        receiverValue?.type?.contains {
            (it as? TypeTemplate)?.coroutineInferenceData?.let { coroutineInferenceData = it }
            false
        }
        return coroutineInferenceData
    }
    return getData(resultingCall.dispatchReceiver) ?: getData(resultingCall.extensionReceiver)
}
