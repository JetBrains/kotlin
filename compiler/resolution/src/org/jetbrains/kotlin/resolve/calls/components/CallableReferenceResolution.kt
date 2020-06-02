/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.components.CreateFreshVariablesSubstitutor.createToFreshVariableSubstitutorAndAddInitialConstraints
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.LowerPriorityToPreserveCompatibility
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.DISPATCH_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.EXTENSION_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.captureFromExpression
import org.jetbrains.kotlin.types.expressions.CoercionStrategy
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull

sealed class CallableReceiver(val receiver: ReceiverValueWithSmartCastInfo) {
    class UnboundReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class BoundValueReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ScopeReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ExplicitValueReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
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
    val callableReferenceAdaptation: CallableReferenceAdaptation?,
    initialDiagnostics: List<KotlinCallDiagnostic>
) : Candidate {
    private val mutableDiagnostics = initialDiagnostics.toMutableList()
    val diagnostics: List<KotlinCallDiagnostic> = mutableDiagnostics

    override val resultingApplicability = getResultApplicability(diagnostics)

    override fun addCompatibilityWarning(other: Candidate) {
        if (this !== other && other is CallableReferenceCandidate) {
            mutableDiagnostics.add(CompatibilityWarning(other.candidate))
        }
    }

    override val isSuccessful get() = resultingApplicability.isSuccess

    var freshSubstitutor: FreshVariableNewTypeSubstitutor? = null
        internal set

    val numDefaults get() = callableReferenceAdaptation?.defaults ?: 0
}

class CallableReferenceAdaptation(
    val argumentTypes: Array<KotlinType>,
    val coercionStrategy: CoercionStrategy,
    val defaults: Int,
    val mappedArguments: Map<ValueParameterDescriptor, ResolvedCallArgument>,
    val suspendConversionStrategy: SuspendConversionStrategy
)

enum class SuspendConversionStrategy {
    SUSPEND_CONVERSION, NO_CONVERSION
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

    if (!ErrorUtils.isError(candidateDescriptor)) {
        addReceiverConstraint(toFreshSubstitutor, dispatchReceiver, candidateDescriptor.dispatchReceiverParameter, position)
        addReceiverConstraint(toFreshSubstitutor, extensionReceiver, candidateDescriptor.extensionReceiverParameter, position)
    }

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
    val receiverType = receiverArgument.receiver.stableType.let { captureFromExpression(it) ?: it }

    addSubtypeConstraint(receiverType, expectedType, position)
}

class CallableReferencesCandidateFactory(
    val argument: CallableReferenceKotlinCallArgument,
    val callComponents: KotlinCallComponents,
    val scopeTower: ImplicitScopeTower,
    val compatibilityChecker: ((ConstraintSystemOperation) -> Unit) -> Unit,
    val expectedType: UnwrappedType?,
    private val csBuilder: ConstraintSystemOperation
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

        val (reflectionCandidateType, callableReferenceAdaptation) = buildReflectionType(
            candidateDescriptor,
            dispatchCallableReceiver,
            extensionCallableReceiver,
            expectedType,
            callComponents.builtIns
        )

        if (needCompatibilityWarning(callableReferenceAdaptation, candidateDescriptor)) {
            diagnostics.add(LowerPriorityToPreserveCompatibility)
        }

        if (callableReferenceAdaptation != null &&
            callableReferenceAdaptation.defaults != 0 &&
            !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType)
        ) {
            diagnostics.add(CallableReferencesDefaultArgumentUsed(argument, candidateDescriptor, callableReferenceAdaptation.defaults))
        }

        if (candidateDescriptor !is CallableMemberDescriptor) {
            return CallableReferenceCandidate(
                candidateDescriptor, dispatchCallableReceiver, extensionCallableReceiver,
                explicitReceiverKind, reflectionCandidateType, callableReferenceAdaptation,
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
            explicitReceiverKind, reflectionCandidateType, callableReferenceAdaptation, diagnostics
        )
    }

    private fun needCompatibilityWarning(
        callableReferenceAdaptation: CallableReferenceAdaptation?,
        candidate: CallableDescriptor
    ): Boolean {
        // KT-13934: reference to companion object member via class name
        if (candidate.containingDeclaration.isCompanionObject() && argument.lhsResult is LHSResult.Type) return true

        if (callableReferenceAdaptation == null) return false

        return callableReferenceAdaptation.defaults != 0 ||
                callableReferenceAdaptation.suspendConversionStrategy != SuspendConversionStrategy.NO_CONVERSION ||
                callableReferenceAdaptation.coercionStrategy != CoercionStrategy.NO_COERCION ||
                callableReferenceAdaptation.mappedArguments.values.any { it is ResolvedCallArgument.VarargArgument }
    }

    private enum class VarargMappingState {
        UNMAPPED, MAPPED_WITH_PLAIN_ARGS, MAPPED_WITH_ARRAY
    }

    private fun getCallableReferenceAdaptation(
        descriptor: FunctionDescriptor,
        expectedType: UnwrappedType?,
        unboundReceiverCount: Int,
        builtins: KotlinBuiltIns
    ): CallableReferenceAdaptation? {
        val inputOutputTypes = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType) ?: return null

        val expectedArgumentCount = inputOutputTypes.inputTypes.size - unboundReceiverCount
        if (expectedArgumentCount < 0) return null

        val fakeArguments = (0 until expectedArgumentCount).map { FakeKotlinCallArgumentForCallableReference(it) }
        val argumentMapping =
            callComponents.argumentsToParametersMapper.mapArguments(fakeArguments, externalArgument = null, descriptor = descriptor)
        if (argumentMapping.diagnostics.any { !it.candidateApplicability.isSuccess }) return null

        /**
         * (A, B, C) -> Unit
         * fun foo(a: A, b: B = B(), vararg c: C)
         */
        var defaults = 0
        var varargMappingState = VarargMappingState.UNMAPPED
        val mappedArguments = linkedMapOf<ValueParameterDescriptor, ResolvedCallArgument>()
        val mappedVarargElements = linkedMapOf<ValueParameterDescriptor, MutableList<KotlinCallArgument>>()
        val mappedArgumentTypes = arrayOfNulls<KotlinType?>(fakeArguments.size)

        for ((valueParameter, resolvedArgument) in argumentMapping.parameterToCallArgumentMap) {
            for (fakeArgument in resolvedArgument.arguments) {
                val index = (fakeArgument as FakeKotlinCallArgumentForCallableReference).index
                val substitutedParameter = descriptor.valueParameters.getOrNull(valueParameter.index) ?: continue

                val mappedArgument: KotlinType?
                if (substitutedParameter.isVararg) {
                    val (varargType, newVarargMappingState) = varargParameterTypeByExpectedParameter(
                        inputOutputTypes.inputTypes[index + unboundReceiverCount],
                        substitutedParameter,
                        varargMappingState,
                        builtins
                    )
                    varargMappingState = newVarargMappingState
                    mappedArgument = varargType

                    when (newVarargMappingState) {
                        VarargMappingState.MAPPED_WITH_ARRAY -> {
                            // If we've already mapped an argument to this value parameter, it'll always be a type mismatch.
                            mappedArguments[valueParameter] = ResolvedCallArgument.SimpleArgument(fakeArgument)
                        }

                        VarargMappingState.MAPPED_WITH_PLAIN_ARGS -> {
                            mappedVarargElements.getOrPut(valueParameter) { ArrayList() }.add(fakeArgument)
                        }
                    }
                } else {
                    mappedArgument = substitutedParameter.type
                    mappedArguments[valueParameter] = resolvedArgument
                }

                mappedArgumentTypes[index] = mappedArgument
            }
            if (resolvedArgument == ResolvedCallArgument.DefaultArgument) {
                defaults++
                mappedArguments[valueParameter] = resolvedArgument
            }
        }
        if (mappedArgumentTypes.any { it == null }) return null

        for ((valueParameter, varargElements) in mappedVarargElements) {
            mappedArguments[valueParameter] = ResolvedCallArgument.VarargArgument(varargElements)
        }

        for (valueParameter in descriptor.valueParameters) {
            if (valueParameter.isVararg && valueParameter !in mappedArguments) {
                mappedArguments[valueParameter] = ResolvedCallArgument.VarargArgument(emptyList())
            }
        }

        // lower(Unit!) = Unit
        val returnExpectedType = inputOutputTypes.outputType

        val coercion =
            if (returnExpectedType.isUnit() && descriptor.returnType?.isUnit() == false)
                CoercionStrategy.COERCION_TO_UNIT
            else
                CoercionStrategy.NO_COERCION

        val adaptedArguments =
            if (expectedType != null && ReflectionTypes.isBaseTypeForNumberedReferenceTypes(expectedType))
                emptyMap()
            else
                mappedArguments

        val suspendConversionStrategy =
            if (!descriptor.isSuspend && expectedType?.isSuspendFunctionType == true) {
                SuspendConversionStrategy.SUSPEND_CONVERSION
            } else {
                SuspendConversionStrategy.NO_CONVERSION
            }

        return CallableReferenceAdaptation(
            @Suppress("UNCHECKED_CAST") (mappedArgumentTypes as Array<KotlinType>),
            coercion, defaults,
            adaptedArguments,
            suspendConversionStrategy
        )
    }

    private fun varargParameterTypeByExpectedParameter(
        expectedParameterType: KotlinType,
        substitutedParameter: ValueParameterDescriptor,
        varargMappingState: VarargMappingState,
        builtins: KotlinBuiltIns
    ): Pair<KotlinType?, VarargMappingState> {
        val elementType = substitutedParameter.varargElementType
            ?: error("Vararg parameter $substitutedParameter does not have vararg type")

        return when (varargMappingState) {
            VarargMappingState.UNMAPPED -> {
                if (KotlinBuiltIns.isArrayOrPrimitiveArray(expectedParameterType) ||
                    csBuilder.isTypeVariable(expectedParameterType)
                ) {
                    val arrayType = builtins.getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(elementType)
                        ?: builtins.getArrayType(Variance.OUT_VARIANCE, elementType)
                    arrayType to VarargMappingState.MAPPED_WITH_ARRAY
                } else {
                    elementType to VarargMappingState.MAPPED_WITH_PLAIN_ARGS
                }
            }
            VarargMappingState.MAPPED_WITH_PLAIN_ARGS -> {
                if (KotlinBuiltIns.isArrayOrPrimitiveArray(expectedParameterType))
                    null to VarargMappingState.MAPPED_WITH_PLAIN_ARGS
                else
                    elementType to VarargMappingState.MAPPED_WITH_PLAIN_ARGS
            }
            VarargMappingState.MAPPED_WITH_ARRAY ->
                null to VarargMappingState.MAPPED_WITH_ARRAY
        }
    }

    private fun buildReflectionType(
        descriptor: CallableDescriptor,
        dispatchReceiver: CallableReceiver?,
        extensionReceiver: CallableReceiver?,
        expectedType: UnwrappedType?,
        builtins: KotlinBuiltIns
    ): Pair<UnwrappedType, CallableReferenceAdaptation?> {
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
                ) to null
            }
            is FunctionDescriptor -> {
                val callableReferenceAdaptation = getCallableReferenceAdaptation(
                    descriptor, expectedType,
                    unboundReceiverCount = argumentsAndReceivers.size,
                    builtins = builtins
                )

                val returnType = if (callableReferenceAdaptation == null) {
                    descriptor.valueParameters.mapTo(argumentsAndReceivers) { it.type }
                    descriptorReturnType
                } else {
                    val arguments = callableReferenceAdaptation.argumentTypes
                    val coercion = callableReferenceAdaptation.coercionStrategy
                    argumentsAndReceivers.addAll(arguments)

                    if (coercion == CoercionStrategy.COERCION_TO_UNIT)
                        descriptor.builtIns.unitType
                    else
                        descriptorReturnType
                }

                val suspendConversionStrategy = callableReferenceAdaptation?.suspendConversionStrategy
                val isSuspend = descriptor.isSuspend || suspendConversionStrategy == SuspendConversionStrategy.SUSPEND_CONVERSION

                return callComponents.reflectionTypes.getKFunctionType(
                    Annotations.EMPTY, null, argumentsAndReceivers, null,
                    returnType, descriptor.builtIns, isSuspend
                ) to callableReferenceAdaptation
            }
            else -> return ErrorUtils.createErrorType("Unsupported descriptor type: $descriptor") to null
        }
    }

    private fun toCallableReceiver(receiver: ReceiverValueWithSmartCastInfo, isExplicit: Boolean): CallableReceiver {
        if (!isExplicit) return CallableReceiver.ScopeReceiver(receiver)

        return when (val lhsResult = argument.lhsResult) {
            is LHSResult.Expression -> CallableReceiver.ExplicitValueReceiver(receiver)
            is LHSResult.Type -> {
                if (lhsResult.qualifier?.classValueReceiver?.type == receiver.receiverValue.type) {
                    CallableReceiver.BoundValueReference(receiver)
                } else {
                    CallableReceiver.UnboundReference(receiver)
                }
            }
            is LHSResult.Object -> CallableReceiver.BoundValueReference(receiver)
            else -> throw IllegalStateException("Unsupported kind of lhsResult: $lhsResult")
        }
    }
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
