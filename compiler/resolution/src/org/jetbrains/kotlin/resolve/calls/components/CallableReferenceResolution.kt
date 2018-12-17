/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.components.CreateFreshVariablesSubstitutor.createToFreshVariableSubstitutorAndAddInitialConstraints
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.DISPATCH_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.EXTENSION_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.CoercionStrategy
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull

sealed class CallableReceiver(val receiver: ReceiverValueWithSmartCastInfo) {
    class UnboundReference(val qualifier: QualifierReceiver, receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class BoundValueReference(val qualifier: QualifierReceiver, receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ScopeReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ExplicitValueReceiver(val lhsArgument: SimpleKotlinCallArgument, receiver: ReceiverValueWithSmartCastInfo) :
        CallableReceiver(receiver)
}

// todo investigate similar code in CheckVisibility
private val CallableReceiver.asReceiverValueForVisibilityChecks: ReceiverValue
    get() = receiver.receiverValue

/**
 * Suppose we have class A with staticM, memberM, memberExtM.
 * For A::staticM both receivers will be null
 * For A::memberM dispatchReceiver = UnboundReceiver, extensionReceiver = null
 * For a::memberExtM dispatchReceiver = ExplicitValueReceiver, extensionReceiver = ExplicitValueReceiver
 *
 * For class B with companion object B::companionM dispatchReceiver = BoundValueReference
 */
class CallableReferenceCandidate(
    val candidate: CallableDescriptor,
    val dispatchReceiver: CallableReceiver?,
    val extensionReceiver: CallableReceiver?,
    val explicitReceiverKind: ExplicitReceiverKind,
    val reflectionCandidateType: UnwrappedType,
    val numDefaults: Int,
    val diagnostics: List<KotlinCallDiagnostic>
) : Candidate {
    override val resultingApplicability = getResultApplicability(diagnostics)
    override val isSuccessful get() = resultingApplicability.isSuccess

    var freshSubstitutor: FreshVariableNewTypeSubstitutor? = null
        internal set
}

/**
 * cases: class A {}, class B { companion object }, object C, enum class D { E }
 * A::foo <-> Type
 * a::foo <-> Expression
 * B::foo <-> Type
 * C::foo <-> Object
 * D.E::foo <-> Expression
 */
fun createCallableReferenceProcessor(factory: CallableReferencesCandidateFactory): ScopeTowerProcessor<CallableReferenceCandidate> {
    val lhsResult = factory.argument.lhsResult
    when (lhsResult) {
        LHSResult.Empty, LHSResult.Error, is LHSResult.Expression -> {
            val explicitReceiver = (lhsResult as? LHSResult.Expression)?.lshCallArgument?.receiver
            return factory.createCallableProcessor(explicitReceiver)
        }
        is LHSResult.Type -> {
            val static = factory.createCallableProcessor(lhsResult.qualifier)
            val unbound = factory.createCallableProcessor(lhsResult.unboundDetailedReceiver)

            // note that if we use PrioritizedCompositeScopeTowerProcessor then static will win over unbound members
            val staticOrUnbound = SamePriorityCompositeScopeTowerProcessor(static, unbound)

            val asValue = lhsResult.qualifier.classValueReceiverWithSmartCastInfo ?: return staticOrUnbound
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

fun ConstraintSystemOperation.checkCallableReference(
    argument: CallableReferenceKotlinCallArgument,
    dispatchReceiver: CallableReceiver?,
    extensionReceiver: CallableReceiver?,
    candidateDescriptor: CallableDescriptor,
    reflectionCandidateType: UnwrappedType,
    expectedType: UnwrappedType?,
    ownerDescriptor: DeclarationDescriptor
): Pair<FreshVariableNewTypeSubstitutor, KotlinCallDiagnostic?> {
    val position = ArgumentConstraintPosition(argument)

    val toFreshSubstitutor = createToFreshVariableSubstitutorAndAddInitialConstraints(candidateDescriptor, this)

    if (expectedType != null) {
        addSubtypeConstraint(toFreshSubstitutor.safeSubstitute(reflectionCandidateType), expectedType, position)
    }

    addReceiverConstraint(toFreshSubstitutor, dispatchReceiver, candidateDescriptor.dispatchReceiverParameter, position)
    addReceiverConstraint(toFreshSubstitutor, extensionReceiver, candidateDescriptor.extensionReceiverParameter, position)

    val invisibleMember = Visibilities.findInvisibleMember(
        dispatchReceiver?.asReceiverValueForVisibilityChecks,
        candidateDescriptor, ownerDescriptor
    )
    return toFreshSubstitutor to invisibleMember?.let(::VisibilityError)
}


private fun ConstraintSystemOperation.addReceiverConstraint(
    toFreshSubstitutor: FreshVariableNewTypeSubstitutor,
    receiverArgument: CallableReceiver?,
    receiverParameter: ReceiverParameterDescriptor?,
    position: ArgumentConstraintPosition
) {
    if (receiverArgument == null || receiverParameter == null) {
        assert(receiverArgument == null) { "Receiver argument should be null if parameter is: $receiverArgument" }
        assert(receiverParameter == null) { "Receiver parameter should be null if argument is: $receiverParameter" }
        return
    }

    val expectedType = toFreshSubstitutor.safeSubstitute(receiverParameter.value.type.unwrap())
    val receiverType = receiverArgument.receiver.stableType
    addSubtypeConstraint(receiverType, expectedType, position)
}

class CallableReferencesCandidateFactory(
    val argument: CallableReferenceKotlinCallArgument,
    val callComponents: KotlinCallComponents,
    val scopeTower: ImplicitScopeTower,
    val compatibilityChecker: ((ConstraintSystemOperation) -> Unit) -> Unit,
    val expectedType: UnwrappedType?
) : CandidateFactory<CallableReferenceCandidate> {

    fun createCallableProcessor(explicitReceiver: DetailedReceiver?) =
        createCallableReferenceProcessor(scopeTower, argument.rhsName, this, explicitReceiver)

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): CallableReferenceCandidate {

        val dispatchCallableReceiver =
            towerCandidate.dispatchReceiver?.let { toCallableReceiver(it, explicitReceiverKind == DISPATCH_RECEIVER) }
        val extensionCallableReceiver = extensionReceiver?.let { toCallableReceiver(it, explicitReceiverKind == EXTENSION_RECEIVER) }
        val candidateDescriptor = towerCandidate.descriptor
        val diagnostics = SmartList<KotlinCallDiagnostic>()

        val (reflectionCandidateType, defaults) = buildReflectionType(
            candidateDescriptor,
            dispatchCallableReceiver,
            extensionCallableReceiver,
            expectedType
        )

        if (defaults != 0) {
            diagnostics.add(CallableReferencesDefaultArgumentUsed(argument, candidateDescriptor, defaults))
        }

        if (candidateDescriptor !is CallableMemberDescriptor) {
            return CallableReferenceCandidate(
                candidateDescriptor, dispatchCallableReceiver, extensionCallableReceiver,
                explicitReceiverKind, reflectionCandidateType, defaults,
                listOf(NotCallableMemberReference(argument, candidateDescriptor))
            )
        }

        diagnostics.addAll(towerCandidate.diagnostics)
        // todo smartcast on receiver diagnostic and CheckInstantiationOfAbstractClass

        compatibilityChecker {
            if (it.hasContradiction) return@compatibilityChecker

            val (_, visibilityError) = it.checkCallableReference(
                argument, dispatchCallableReceiver, extensionCallableReceiver, candidateDescriptor,
                reflectionCandidateType, expectedType, scopeTower.lexicalScope.ownerDescriptor
            )

            diagnostics.addIfNotNull(visibilityError)

            if (it.hasContradiction) diagnostics.add(
                CallableReferenceNotCompatible(
                    argument,
                    candidateDescriptor,
                    expectedType,
                    reflectionCandidateType
                )
            )
        }

        return CallableReferenceCandidate(
            candidateDescriptor, dispatchCallableReceiver, extensionCallableReceiver,
            explicitReceiverKind, reflectionCandidateType, defaults, diagnostics
        )
    }

    private fun getArgumentAndReturnTypeUseMappingByExpectedType(
        descriptor: FunctionDescriptor,
        expectedType: UnwrappedType?,
        unboundReceiverCount: Int
    ): Triple<Array<KotlinType>, CoercionStrategy, Int>? {
        val inputOutputTypes = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType) ?: return null

        val expectedArgumentCount = inputOutputTypes.inputTypes.size - unboundReceiverCount
        if (expectedArgumentCount < 0) return null

        val fakeArguments = (0..(expectedArgumentCount - 1)).map { FakeKotlinCallArgumentForCallableReference(it) }
        val argumentMapping =
            callComponents.argumentsToParametersMapper.mapArguments(fakeArguments, externalArgument = null, descriptor = descriptor)
        if (argumentMapping.diagnostics.any { !it.candidateApplicability.isSuccess }) return null

        /**
         * (A, B, C) -> Unit
         * fun foo(a: A, b: B = B(), vararg c: C)
         */
        var defaults = 0
        val mappedArguments = arrayOfNulls<KotlinType?>(fakeArguments.size)
        for ((valueParameter, resolvedArgument) in argumentMapping.parameterToCallArgumentMap) {
            for (fakeArgument in resolvedArgument.arguments) {
                val index = (fakeArgument as FakeKotlinCallArgumentForCallableReference).index
                val substitutedParameter = descriptor.valueParameters.getOrNull(valueParameter.index) ?: continue

                mappedArguments[index] = substitutedParameter.varargElementType ?: substitutedParameter.type
            }
            if (resolvedArgument == ResolvedCallArgument.DefaultArgument) defaults++
        }
        if (mappedArguments.any { it == null }) return null

        // lower(Unit!) = Unit
        val returnExpectedType = inputOutputTypes.outputType

        val coercion = if (returnExpectedType.isUnit()) CoercionStrategy.COERCION_TO_UNIT else CoercionStrategy.NO_COERCION

        @Suppress("UNCHECKED_CAST")
        return Triple(mappedArguments as Array<KotlinType>, coercion, defaults)
    }

    private fun buildReflectionType(
        descriptor: CallableDescriptor,
        dispatchReceiver: CallableReceiver?,
        extensionReceiver: CallableReceiver?,
        expectedType: UnwrappedType?
    ): Pair<UnwrappedType, /*defaults*/ Int> {
        val argumentsAndReceivers = ArrayList<KotlinType>(descriptor.valueParameters.size + 2)

        if (dispatchReceiver is CallableReceiver.UnboundReference) {
            argumentsAndReceivers.add(dispatchReceiver.receiver.stableType)
        }
        if (extensionReceiver is CallableReceiver.UnboundReference) {
            argumentsAndReceivers.add(extensionReceiver.receiver.stableType)
        }

        val descriptorReturnType = descriptor.returnType
                ?: ErrorUtils.createErrorType("Error return type for descriptor: $descriptor")

        when (descriptor) {
            is PropertyDescriptor -> {
                val mutable = descriptor.isVar && run {
                    val setter = descriptor.setter
                    setter == null || Visibilities.isVisible(
                        dispatchReceiver?.asReceiverValueForVisibilityChecks, setter,
                        scopeTower.lexicalScope.ownerDescriptor
                    )
                }

                return callComponents.reflectionTypes.getKPropertyType(
                    Annotations.EMPTY,
                    argumentsAndReceivers,
                    descriptorReturnType,
                    mutable
                ) to 0
            }
            is FunctionDescriptor -> {
                val returnType: KotlinType
                val defaults: Int
                val argumentsAndExpectedTypeCoercion = getArgumentAndReturnTypeUseMappingByExpectedType(
                    descriptor, expectedType,
                    unboundReceiverCount = argumentsAndReceivers.size
                )

                if (argumentsAndExpectedTypeCoercion == null) {
                    descriptor.valueParameters.mapTo(argumentsAndReceivers) { it.type }
                    returnType = descriptorReturnType
                    defaults = 0
                } else {
                    val (arguments, coercion) = argumentsAndExpectedTypeCoercion
                    defaults = argumentsAndExpectedTypeCoercion.third
                    argumentsAndReceivers.addAll(arguments)

                    returnType = if (coercion == CoercionStrategy.COERCION_TO_UNIT) descriptor.builtIns.unitType else descriptorReturnType
                }

                return callComponents.reflectionTypes.getKFunctionType(
                    Annotations.EMPTY, null, argumentsAndReceivers, null,
                    returnType, descriptor.builtIns, isSuspend = false
                ) to defaults
            }
            else -> error("Unsupported descriptor type: $descriptor")
        }
    }

    private fun toCallableReceiver(receiver: ReceiverValueWithSmartCastInfo, isExplicit: Boolean): CallableReceiver {
        if (!isExplicit) return CallableReceiver.ScopeReceiver(receiver)

        val lhsResult = argument.lhsResult
        return when (lhsResult) {
            is LHSResult.Expression -> CallableReceiver.ExplicitValueReceiver(lhsResult.lshCallArgument, receiver)
            is LHSResult.Type -> {
                if (lhsResult.qualifier.classValueReceiver?.type == receiver.receiverValue.type) {
                    CallableReceiver.BoundValueReference(lhsResult.qualifier, receiver)
                } else {
                    CallableReceiver.UnboundReference(lhsResult.qualifier, receiver)
                }
            }
            is LHSResult.Object -> CallableReceiver.BoundValueReference(lhsResult.qualifier, receiver)
            else -> throw IllegalStateException("Unsupported kind of lhsResult: $lhsResult")
        }
    }
}

data class InputOutputTypes(val inputTypes: List<UnwrappedType>, val outputType: UnwrappedType)

fun extractInputOutputTypesFromCallableReferenceExpectedType(expectedType: UnwrappedType?): InputOutputTypes? {
    if (expectedType == null) return null

    return when {
        expectedType.isFunctionType ->
            extractInputOutputTypesFromFunctionType(expectedType)

        ReflectionTypes.isBaseTypeForNumberedReferenceTypes(expectedType) ->
            InputOutputTypes(emptyList(), expectedType.arguments.single().type.unwrap())

        ReflectionTypes.isNumberedKFunctionOrKSuspendFunction(expectedType) -> {
            val functionFromSupertype = expectedType.immediateSupertypes().first { it.isFunctionType }.unwrap()
            extractInputOutputTypesFromFunctionType(functionFromSupertype)
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
