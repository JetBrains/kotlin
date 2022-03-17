/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.checker.captureFromExpression
import org.jetbrains.kotlin.types.expressions.CoercionStrategy
import org.jetbrains.kotlin.types.model.TypeVariance
import org.jetbrains.kotlin.types.model.convertVariance
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.supertypes

sealed class CallableReceiver(val receiver: ReceiverValueWithSmartCastInfo) {
    class UnboundReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class BoundValueReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ScopeReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ExplicitValueReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
}

class CallableReferenceAdaptation(
    val argumentTypes: Array<KotlinType>,
    val coercionStrategy: CoercionStrategy,
    val defaults: Int,
    val mappedArguments: Map<ValueParameterDescriptor, ResolvedCallArgument>,
    val suspendConversionStrategy: SuspendConversionStrategy
)

/**
 * cases: class A {}, class B { companion object }, object C, enum class D { E }
 * A::foo <-> Type
 * a::foo <-> Expression
 * B::foo <-> Type
 * C::foo <-> Object
 * D.E::foo <-> Expression
 */
fun createCallableReferenceProcessor(factory: CallableReferencesCandidateFactory): ScopeTowerProcessor<CallableReferenceResolutionCandidate> {
    when (val lhsResult = factory.kotlinCall.lhsResult) {
        LHSResult.Empty, LHSResult.Error, is LHSResult.Expression -> {
            val explicitReceiver = (lhsResult as? LHSResult.Expression)?.lshCallArgument?.receiver
            return factory.createCallableProcessor(explicitReceiver)
        }
        is LHSResult.Type -> {
            val static = lhsResult.qualifier?.let(factory::createCallableProcessor)
            val unbound = factory.createCallableProcessor(lhsResult.unboundDetailedReceiver)

            // note that if we use PrioritizedCompositeScopeTowerProcessor then static will win over unbound members
            val staticOrUnbound =
                if (static != null)
                    SamePriorityCompositeScopeTowerProcessor(static, unbound)
                else
                    unbound

            val asValue = lhsResult.qualifier?.classValueReceiverWithSmartCastInfo ?: return staticOrUnbound
            return PrioritizedCompositeScopeTowerProcessor(staticOrUnbound, factory.createCallableProcessor(asValue))
        }
        is LHSResult.Object -> {
            // callable reference to nested class constructor
            val static = factory.createCallableProcessor(lhsResult.qualifier)
            val boundObjectReference = factory.createCallableProcessor(lhsResult.objectValueReceiver)

            return SamePriorityCompositeScopeTowerProcessor(static, boundObjectReference)
        }
    }
}

fun CallableReferenceResolutionCandidate.addConstraints(
    constraintSystem: ConstraintSystemOperation,
    substitutor: FreshVariableNewTypeSubstitutor,
    callableReference: CallableReferenceResolutionAtom
) {
    val lhsResult = callableReference.lhsResult
    val position = when (callableReference) {
        is CallableReferenceKotlinCallArgument -> ArgumentConstraintPositionImpl(callableReference)
        is CallableReferenceKotlinCall -> CallableReferenceConstraintPositionImpl(callableReference)
    }

    if (lhsResult is LHSResult.Type && expectedType != null && !TypeUtils.noExpectedType(expectedType)) {
        // NB: regular objects have lhsResult of `LHSResult.Object` type and won't be proceeded here
        val isStaticOrCompanionMember =
            DescriptorUtils.isStaticDeclaration(candidate) || candidate.containingDeclaration.isCompanionObject()
        if (!isStaticOrCompanionMember) {
            constraintSystem.addLhsTypeConstraint(lhsResult.unboundDetailedReceiver.stableType, expectedType, position)
        }
    }

    if (!ErrorUtils.isError(candidate)) {
        constraintSystem.addReceiverConstraint(substitutor, dispatchReceiver, candidate.dispatchReceiverParameter, position)
        constraintSystem.addReceiverConstraint(substitutor, extensionReceiver, candidate.extensionReceiverParameter, position)
    }

    if (expectedType != null && !TypeUtils.noExpectedType(expectedType) && !constraintSystem.hasContradiction) {
        constraintSystem.addSubtypeConstraint(substitutor.safeSubstitute(reflectionCandidateType), expectedType, position)
    }
}

private fun ConstraintSystemOperation.addLhsTypeConstraint(
    lhsType: KotlinType,
    expectedType: UnwrappedType,
    position: ConstraintPosition
) {
    if (!ReflectionTypes.isNumberedTypeWithOneOrMoreNumber(expectedType)) return

    val expectedTypeProjectionForLHS = expectedType.arguments.first()
    val expectedTypeForLHS = expectedTypeProjectionForLHS.type
    val expectedTypeVariance = expectedTypeProjectionForLHS.projectionKind.convertVariance()
    val effectiveVariance = AbstractTypeChecker.effectiveVariance(
        expectedType.constructor.parameters.first().variance.convertVariance(),
        expectedTypeVariance
    ) ?: expectedTypeVariance

    when (effectiveVariance) {
        TypeVariance.INV -> addEqualityConstraint(lhsType, expectedTypeForLHS, position)
        TypeVariance.IN -> addSubtypeConstraint(expectedTypeForLHS, lhsType, position)
        TypeVariance.OUT -> addSubtypeConstraint(lhsType, expectedTypeForLHS, position)
    }
}

private fun ConstraintSystemOperation.addReceiverConstraint(
    toFreshSubstitutor: FreshVariableNewTypeSubstitutor,
    receiverArgument: CallableReceiver?,
    receiverParameter: ReceiverParameterDescriptor?,
    position: ConstraintPosition
) {
    if (receiverArgument == null || receiverParameter == null) {
        assert(receiverArgument == null) { "Receiver argument should be null if parameter is: $receiverArgument" }
        assert(receiverParameter == null) { "Receiver parameter should be null if argument is: $receiverParameter" }
        return
    }

    val expectedType = toFreshSubstitutor.safeSubstitute(receiverParameter.value.type.unwrap())
    val receiverType = receiverArgument.receiver.stableType.let { captureFromExpression(it) ?: it }

    addSubtypeConstraint(receiverType, expectedType, position)
}

data class InputOutputTypes(val inputTypes: List<UnwrappedType>, val outputType: UnwrappedType)

fun extractInputOutputTypesFromCallableReferenceExpectedType(expectedType: UnwrappedType?): InputOutputTypes? {
    if (expectedType == null) return null

    return when {
        expectedType.isFunctionType || expectedType.isSuspendFunctionType ->
            extractInputOutputTypesFromFunctionType(expectedType)

        ReflectionTypes.isBaseTypeForNumberedReferenceTypes(expectedType) ->
            InputOutputTypes(emptyList(), expectedType.arguments.single().type.unwrap())

        ReflectionTypes.isNumberedKFunction(expectedType) -> {
            val functionFromSupertype = expectedType.immediateSupertypes().first { it.isFunctionType }.unwrap()
            extractInputOutputTypesFromFunctionType(functionFromSupertype)
        }

        ReflectionTypes.isNumberedKSuspendFunction(expectedType) -> {
            val kSuspendFunctionType = expectedType.immediateSupertypes().first { it.isSuspendFunctionType }.unwrap()
            extractInputOutputTypesFromFunctionType(kSuspendFunctionType)
        }

        ReflectionTypes.isNumberedKPropertyOrKMutablePropertyType(expectedType) -> {
            val functionFromSupertype = expectedType.supertypes().first { it.isFunctionType }.unwrap()
            extractInputOutputTypesFromFunctionType(functionFromSupertype)
        }

        else -> null
    }
}

private fun extractInputOutputTypesFromFunctionType(functionType: UnwrappedType): InputOutputTypes {
    val receiver = functionType.getReceiverTypeFromFunctionType()?.unwrap()
    val parameters = functionType.getValueParameterTypesFromFunctionType().map { it.type.unwrap() }

    val inputTypes = listOfNotNull(receiver) + parameters
    val outputType = functionType.getReturnTypeFromFunctionType().unwrap()

    return InputOutputTypes(inputTypes, outputType)
}
