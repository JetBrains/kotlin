/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.CoercionStrategy
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull

class CallableReferencesCandidateFactory(
    val argument: CallableReferenceKotlinCallArgument,
    val callComponents: KotlinCallComponents,
    val scopeTower: ImplicitScopeTower,
    val compatibilityChecker: ((ConstraintSystemOperation) -> Unit) -> Unit,
    val expectedType: UnwrappedType?,
    private val csBuilder: ConstraintSystemOperation,
    private val resolutionCallbacks: KotlinResolutionCallbacks
) : CandidateFactory<CallableReferenceCandidate> {

    fun createCallableProcessor(explicitReceiver: DetailedReceiver?) =
        createCallableReferenceProcessor(scopeTower, argument.rhsName, this, explicitReceiver)

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): CallableReferenceCandidate {

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
            callComponents.builtIns
        )

        fun createReferenceCandidate(): CallableReferenceCandidate =
            CallableReferenceCandidate(
                candidateDescriptor, dispatchCallableReceiver, extensionCallableReceiver,
                explicitReceiverKind, reflectionCandidateType, callableReferenceAdaptation, diagnostics
            )

        if (callComponents.statelessCallbacks.isHiddenInResolution(candidateDescriptor, argument, resolutionCallbacks)) {
            diagnostics.add(HiddenDescriptor)
            return createReferenceCandidate()
        }

        if (needCompatibilityResolveForCallableReference(callableReferenceAdaptation, candidateDescriptor)) {
            markCandidateForCompatibilityResolve(diagnostics)
        }

        if (callableReferenceAdaptation != null && expectedType != null && hasNonTrivialAdaptation(callableReferenceAdaptation)) {
            if (!expectedType.isFunctionType && !expectedType.isSuspendFunctionType) { // expectedType has some reflection type
                diagnostics.add(AdaptedCallableReferenceIsUsedWithReflection(argument))
            }
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

        return createReferenceCandidate()
    }

    private fun needCompatibilityResolveForCallableReference(
        callableReferenceAdaptation: CallableReferenceAdaptation?,
        candidate: CallableDescriptor
    ): Boolean {
        // KT-13934: reference to companion object member via class name
        if (candidate.containingDeclaration.isCompanionObject() && argument.lhsResult is LHSResult.Type) return true

        if (callableReferenceAdaptation == null) return false

        return hasNonTrivialAdaptation(callableReferenceAdaptation)
    }

    private fun hasNonTrivialAdaptation(callableReferenceAdaptation: CallableReferenceAdaptation) =
        callableReferenceAdaptation.defaults != 0 ||
                callableReferenceAdaptation.suspendConversionStrategy != SuspendConversionStrategy.NO_CONVERSION ||
                callableReferenceAdaptation.coercionStrategy != CoercionStrategy.NO_COERCION ||
                callableReferenceAdaptation.mappedArguments.values.any { it is ResolvedCallArgument.VarargArgument }

    private enum class VarargMappingState {
        UNMAPPED, MAPPED_WITH_PLAIN_ARGS, MAPPED_WITH_ARRAY
    }

    private fun getCallableReferenceAdaptation(
        descriptor: FunctionDescriptor,
        expectedType: UnwrappedType?,
        unboundReceiverCount: Int,
        builtins: KotlinBuiltIns
    ): CallableReferenceAdaptation? {
        if (callComponents.languageVersionSettings.apiVersion < ApiVersion.KOTLIN_1_4) return null

        if (expectedType == null) return null

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

        return when (descriptor) {
            is PropertyDescriptor -> {
                val mutable = descriptor.isVar && run {
                    val setter = descriptor.setter
                    setter == null || DescriptorVisibilities.isVisible(
                        dispatchReceiver?.asReceiverValueForVisibilityChecks, setter,
                        scopeTower.lexicalScope.ownerDescriptor
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

                callComponents.reflectionTypes.getKFunctionType(
                    Annotations.EMPTY, null, argumentsAndReceivers, null,
                    returnType, descriptor.builtIns, isSuspend
                ) to callableReferenceAdaptation
            }
            else -> {
                assert(!descriptor.isSupportedForCallableReference()) { "${descriptor::class} isn't supported to use in callable references actually, but it's listed in `isSupportedForCallableReference` method" }
                ErrorUtils.createErrorType("Unsupported descriptor type: $descriptor") to null
            }
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