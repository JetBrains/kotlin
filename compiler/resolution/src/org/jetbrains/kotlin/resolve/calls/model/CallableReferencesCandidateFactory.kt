/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorScopeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.expressions.CoercionStrategy
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.SmartList

class CallableReferencesCandidateFactory(
    val kotlinCall: CallableReferenceResolutionAtom,
    val callComponents: KotlinCallComponents,
    val scopeTower: ImplicitScopeTower,
    val expectedType: UnwrappedType?,
    private val baseSystem: ConstraintStorage?,
    private val resolutionCallbacks: KotlinResolutionCallbacks
) : CandidateFactory<CallableReferenceResolutionCandidate> {
    // todo investigate similar code in CheckVisibility
    private val CallableReceiver.asReceiverValueForVisibilityChecks: ReceiverValue
        get() = receiver.receiverValue

    override fun createErrorCandidate(): CallableReferenceResolutionCandidate {
        val errorScope = ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_RESOLUTION_CANDIDATE, kotlinCall.toString())
        val errorDescriptor = errorScope.getContributedFunctions(kotlinCall.rhsName, scopeTower.location).first()

        val (reflectionCandidateType, callableReferenceAdaptation) = buildReflectionType(
            errorDescriptor,
            dispatchReceiver = null,
            extensionReceiver = null,
            expectedType,
            callComponents.builtIns,
            buildTypeWithConversions = kotlinCall is CallableReferenceKotlinCallArgument
        )

        return CallableReferenceResolutionCandidate(
            errorDescriptor, dispatchReceiver = null, extensionReceiver = null,
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, reflectionCandidateType, callableReferenceAdaptation,
            kotlinCall, expectedType, callComponents, scopeTower, resolutionCallbacks, baseSystem
        )
    }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): CallableReferenceResolutionCandidate {
        val dispatchCallableReceiver =
            towerCandidate.dispatchReceiver?.let { toCallableReceiver(it, explicitReceiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER) }
        val extensionCallableReceiver = extensionReceiver?.let { toCallableReceiver(it, explicitReceiverKind == ExplicitReceiverKind.EXTENSION_RECEIVER) }
        val candidateDescriptor = towerCandidate.descriptor
        val diagnostics = SmartList<KotlinCallDiagnostic>()

        val (reflectionCandidateType, callableReferenceAdaptation) = buildReflectionType(
            candidateDescriptor,
            dispatchCallableReceiver,
            extensionCallableReceiver,
            expectedType,
            callComponents.builtIns,
            // conversions aren't needed for top-level callable references
            buildTypeWithConversions = kotlinCall is CallableReferenceKotlinCallArgument
        )

        fun createCallableReferenceCallCandidate(diagnostics: List<KotlinCallDiagnostic>) = CallableReferenceResolutionCandidate(
            candidateDescriptor, dispatchCallableReceiver, extensionCallableReceiver,
            explicitReceiverKind, reflectionCandidateType, callableReferenceAdaptation,
            kotlinCall, expectedType, callComponents, scopeTower, resolutionCallbacks, baseSystem
        ).also { diagnostics.forEach(it::addDiagnostic) }

        if (callComponents.statelessCallbacks.isHiddenInResolution(candidateDescriptor, kotlinCall.call, resolutionCallbacks)) {
            diagnostics.add(HiddenDescriptor)
            return createCallableReferenceCallCandidate(diagnostics)
        }

        if (needCompatibilityResolveForCallableReference(callableReferenceAdaptation, candidateDescriptor)) {
            markCandidateForCompatibilityResolve(diagnostics)
        }

        if (callableReferenceAdaptation != null && expectedType != null && hasNonTrivialAdaptation(callableReferenceAdaptation)) {
            if (!expectedType.isFunctionType && !expectedType.isSuspendFunctionType) { // expectedType has some reflection type
                diagnostics.add(AdaptedCallableReferenceIsUsedWithReflection(kotlinCall))
            }
        }

        if (callableReferenceAdaptation != null &&
            callableReferenceAdaptation.defaults != 0 &&
            !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType)
        ) {
            diagnostics.add(CallableReferencesDefaultArgumentUsed(kotlinCall, candidateDescriptor, callableReferenceAdaptation.defaults))
        }

        if (candidateDescriptor !is CallableMemberDescriptor) {
            return createCallableReferenceCallCandidate(listOf(NotCallableMemberReference(kotlinCall, candidateDescriptor)))
        }

        diagnostics.addAll(towerCandidate.diagnostics)
        // todo smartcast on receiver diagnostic and CheckInstantiationOfAbstractClass

        return createCallableReferenceCallCandidate(diagnostics)
    }

    /**
     * The function is called only inside [NoExplicitReceiverScopeTowerProcessor] with [TowerData.BothTowerLevelAndContextReceiversGroup].
     * This case involves only [SimpleCandidateFactory].
     */
    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiverCandidates: List<ReceiverValueWithSmartCastInfo>
    ): CallableReferenceResolutionCandidate =
        error("${this::class.simpleName} doesn't support candidates with multiple extension receiver candidates")

    fun createCallableProcessor(explicitReceiver: DetailedReceiver?) =
        createCallableReferenceProcessor(scopeTower, kotlinCall.rhsName, this, explicitReceiver)

    private fun needCompatibilityResolveForCallableReference(
        callableReferenceAdaptation: CallableReferenceAdaptation?,
        candidate: CallableDescriptor
    ): Boolean {
        // KT-13934: reference to companion object member via class name
        if (candidate.containingDeclaration.isCompanionObject() && kotlinCall.lhsResult is LHSResult.Type) return true

        if (callableReferenceAdaptation == null) return false

        return hasNonTrivialAdaptation(callableReferenceAdaptation)
    }

    private fun hasNonTrivialAdaptation(callableReferenceAdaptation: CallableReferenceAdaptation) =
        callableReferenceAdaptation.defaults != 0 ||
                callableReferenceAdaptation.suspendConversionStrategy != SuspendConversionStrategy.NO_CONVERSION ||
                callableReferenceAdaptation.coercionStrategy != CoercionStrategy.NO_COERCION ||
                callableReferenceAdaptation.mappedArguments.values.any { it is ResolvedCallArgument.VarargArgument }

    private fun getCallableReferenceAdaptation(
        descriptor: FunctionDescriptor,
        expectedType: UnwrappedType?,
        unboundReceiverCount: Int,
        builtins: KotlinBuiltIns
    ): CallableReferenceAdaptation? {
        if (expectedType == null || TypeUtils.noExpectedType(expectedType)) return null

        // Do not adapt references against KCallable type as it's impossible to map defaults/vararg to absent parameters of KCallable
        if (ReflectionTypes.hasKCallableTypeFqName(expectedType)) return null

        val inputOutputTypes = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType) ?: return null

        val expectedArgumentCount = inputOutputTypes.inputTypes.size - unboundReceiverCount
        if (expectedArgumentCount < 0) return null

        val fakeArguments = createFakeArgumentsForReference(descriptor, expectedArgumentCount, inputOutputTypes, unboundReceiverCount)
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
                        VarargMappingState.UNMAPPED -> {
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
            if (valueParameter.isVararg) {
                mappedArguments.putIfAbsent(valueParameter.original, ResolvedCallArgument.VarargArgument(emptyList()))
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
            if (ReflectionTypes.isBaseTypeForNumberedReferenceTypes(expectedType))
                emptyMap()
            else
                mappedArguments

        val suspendConversionStrategy =
            if (!descriptor.isSuspend && expectedType.isSuspendFunctionType) {
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

    private fun createFakeArgumentsForReference(
        descriptor: FunctionDescriptor,
        expectedArgumentCount: Int,
        inputOutputTypes: InputOutputTypes,
        unboundReceiverCount: Int
    ): List<FakeKotlinCallArgumentForCallableReference> {
        var afterVararg = false
        var varargComponentType: UnwrappedType? = null
        var vararg = false
        return (0 until expectedArgumentCount).map { index ->
            val inputType = inputOutputTypes.inputTypes.getOrNull(index + unboundReceiverCount)
            if (vararg && varargComponentType != inputType) {
                afterVararg = true
            }

            val valueParameter = descriptor.valueParameters.getOrNull(index)
            val name =
                if (afterVararg && valueParameter?.declaresDefaultValue() == true)
                    valueParameter.name
                else
                    null

            if (valueParameter?.isVararg == true) {
                varargComponentType = inputType
                vararg = true
            }
            FakeKotlinCallArgumentForCallableReference(index, name)
        }
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
                    expectedParameterType.constructor is TypeVariableTypeConstructor
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
        builtins: KotlinBuiltIns,
        buildTypeWithConversions: Boolean = true
    ): Pair<UnwrappedType, CallableReferenceAdaptation?> {
        val argumentsAndReceivers = ArrayList<KotlinType>(descriptor.valueParameters.size + 2 + descriptor.contextReceiverParameters.size)

        val contextReceiversTypes = descriptor.contextReceiverParameters.map { it.type }
        argumentsAndReceivers.addAll(contextReceiversTypes)

        if (dispatchReceiver is CallableReceiver.UnboundReference) {
            argumentsAndReceivers.add(dispatchReceiver.receiver.stableType)
        }
        if (extensionReceiver is CallableReceiver.UnboundReference) {
            argumentsAndReceivers.add(extensionReceiver.receiver.stableType)
        }

        val descriptorReturnType = descriptor.returnType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE, descriptor.toString())

        return when (descriptor) {
            is PropertyDescriptor -> {
                val mutable = descriptor.isVar && run {
                    val setter = descriptor.setter
                    setter == null || DescriptorVisibilities.isVisible(
                        dispatchReceiver?.asReceiverValueForVisibilityChecks, setter,
                        scopeTower.lexicalScope.ownerDescriptor, false
                    )
                }

                callComponents.reflectionTypes.getKPropertyType(
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

                val returnType = if (callableReferenceAdaptation == null || !buildTypeWithConversions) {
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
                val isSuspend = descriptor.isSuspend ||
                        (suspendConversionStrategy == SuspendConversionStrategy.SUSPEND_CONVERSION && buildTypeWithConversions)

                callComponents.reflectionTypes.getKFunctionType(
                    Annotations.EMPTY, null, emptyList(), argumentsAndReceivers, null,
                    returnType, descriptor.builtIns, isSuspend
                ) to callableReferenceAdaptation
            }
            else -> {
                assert(!descriptor.isSupportedForCallableReference()) { "${descriptor::class} isn't supported to use in callable references actually, but it's listed in `isSupportedForCallableReference` method" }
                ErrorUtils.createErrorType(ErrorTypeKind.UNSUPPORTED_CALLABLE_REFERENCE_TYPE, descriptor.toString()) to null
            }
        }
    }

    private fun toCallableReceiver(receiver: ReceiverValueWithSmartCastInfo, isExplicit: Boolean): CallableReceiver {
        if (!isExplicit) return CallableReceiver.ScopeReceiver(receiver)

        return when (val lhsResult = kotlinCall.lhsResult) {
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

    private enum class VarargMappingState {
        UNMAPPED, MAPPED_WITH_PLAIN_ARGS, MAPPED_WITH_ARRAY
    }
}
