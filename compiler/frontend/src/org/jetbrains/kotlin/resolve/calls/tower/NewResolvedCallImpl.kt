/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.util.toResolutionStatus
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.resolve.scopes.receivers.CastImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class NewResolvedCallImpl<D : CallableDescriptor>(
    override val resolvedCallAtom: ResolvedCallAtom,
    substitutor: NewTypeSubstitutor?,
    private var diagnostics: Collection<KotlinCallDiagnostic>,
    override val typeApproximator: TypeApproximator,
    override val languageVersionSettings: LanguageVersionSettings,
) : NewAbstractResolvedCall<D>() {
    override val psiKotlinCall: PSIKotlinCall = resolvedCallAtom.atom.psiKotlinCall
    override val kotlinCall: KotlinCall = resolvedCallAtom.atom

    override val freshSubstitutor: FreshVariableNewTypeSubstitutor
        get() = resolvedCallAtom.freshVariablesSubstitutor

    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = resolvedCallAtom.argumentMappingByOriginal

    private lateinit var resultingDescriptor: D
    private lateinit var typeArguments: List<UnwrappedType>
    private var smartCastDispatchReceiverType: KotlinType? = null
    private var expectedTypeForSamConvertedArgumentMap: Map<ValueArgument, UnwrappedType>? = null
    private var expectedTypeForSuspendConvertedArgumentMap: Map<ValueArgument, UnwrappedType>? = null
    private var expectedTypeForUnitConvertedArgumentMap: Map<ValueArgument, UnwrappedType>? = null
    private var argumentTypeForConstantConvertedMap: Map<KtExpression, IntegerValueTypeConstant>? = null
    private var extensionReceiver = resolvedCallAtom.extensionReceiverArgument?.receiver?.receiverValue
    private var dispatchReceiver = resolvedCallAtom.dispatchReceiverArgument?.receiver?.receiverValue
    private var contextReceivers = resolvedCallAtom.contextReceiversArguments.map { it.receiver.receiverValue }

    override fun getExtensionReceiver(): ReceiverValue? = extensionReceiver
    override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver
    override fun getContextReceivers(): List<ReceiverValue> = contextReceivers

    @Suppress("UNCHECKED_CAST")
    override fun getCandidateDescriptor(): D = resolvedCallAtom.candidateDescriptor as D
    override fun getResultingDescriptor(): D = resultingDescriptor
    override fun getExplicitReceiverKind(): ExplicitReceiverKind = resolvedCallAtom.explicitReceiverKind

    override fun updateDispatchReceiverType(newType: KotlinType) {
        if (dispatchReceiver?.type == newType) return
        dispatchReceiver = dispatchReceiver?.replaceType(newType)
    }

    override fun updateExtensionReceiverType(newType: KotlinType) {
        if (extensionReceiver?.type == newType) return
        extensionReceiver = extensionReceiver?.replaceType(newType)
    }

    override fun updateContextReceiverTypes(newTypes: List<KotlinType>) {
        if (contextReceivers.size != newTypes.size) return
        contextReceivers = contextReceivers.zip(newTypes).map { (receiver, type) -> receiver.replaceType(type) }
    }

    override fun getStatus(): ResolutionStatus = getResultApplicability(diagnostics).toResolutionStatus()

    override fun getTypeArguments(): Map<TypeParameterDescriptor, KotlinType> {
        val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
        return typeParameters.zip(typeArguments).toMap()
    }

    override fun containsOnlyOnlyInputTypesErrors() =
        diagnostics.all { it is KotlinConstraintSystemDiagnostic && it.error is OnlyInputTypesDiagnostic }

    override fun getSmartCastDispatchReceiverType(): KotlinType? = smartCastDispatchReceiverType

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        //clear cached values
        updateArgumentsMapping(null)
        updateValueArguments(null)

        substituteReceivers(substitutor)

        @Suppress("UNCHECKED_CAST")
        resultingDescriptor = substitutedResultingDescriptor(substitutor) as D

        typeArguments = freshSubstitutor.freshVariables.map {
            val substituted = (substitutor ?: FreshVariableNewTypeSubstitutor.Empty).safeSubstitute(it.defaultType)
            typeApproximator
                .approximateToSuperType(substituted, TypeApproximatorConfiguration.IntegerLiteralsTypesApproximation)
                ?: substituted
        }

        calculateExpectedTypeForSamConvertedArgumentMap(substitutor)
        calculateExpectedTypeForSuspendConvertedArgumentMap(substitutor)
        calculateExpectedTypeForUnitConvertedArgumentMap(substitutor)
        calculateExpectedTypeForConstantConvertedArgumentMap()
    }

    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>,
    ): Map<ValueArgument, ArgumentMatchImpl> {
        val argumentErrors = collectErrorPositions()

        return LinkedHashMap<ValueArgument, ArgumentMatchImpl>().also { result ->
            for (parameter in resultingDescriptor.valueParameters) {
                val resolvedArgument = valueArguments[parameter] ?: continue
                for (argument in resolvedArgument.arguments) {
                    val status = argumentErrors[argument]?.let {
                        ArgumentMatchStatus.TYPE_MISMATCH
                    } ?: ArgumentMatchStatus.SUCCESS
                    result[argument] = ArgumentMatchImpl(parameter).apply { recordMatchStatus(status) }
                }
            }
        }
    }

    fun updateExtensionReceiverWithSmartCastIfNeeded(smartCastExtensionReceiverType: KotlinType) {
        if (extensionReceiver is ImplicitClassReceiver) {
            extensionReceiver = CastImplicitClassReceiver(
                (extensionReceiver as ImplicitClassReceiver).classDescriptor,
                smartCastExtensionReceiverType,
            )
        }
    }

    fun setSmartCastDispatchReceiverType(smartCastDispatchReceiverType: KotlinType) {
        this.smartCastDispatchReceiverType = smartCastDispatchReceiverType
    }

    fun updateDiagnostics(completedDiagnostics: Collection<KotlinCallDiagnostic>) {
        diagnostics = completedDiagnostics
    }

    fun getArgumentTypeForConstantConvertedArgument(valueArgument: ValueArgument): IntegerValueTypeConstant? {
        val expression = valueArgument.getArgumentExpression() ?: return null
        return argumentTypeForConstantConvertedMap?.get(expression)
    }

    fun getExpectedTypeForSamConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expectedTypeForSamConvertedArgumentMap?.get(valueArgument)

    fun getExpectedTypeForSuspendConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expectedTypeForSuspendConvertedArgumentMap?.get(valueArgument)

    fun getExpectedTypeForUnitConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expectedTypeForUnitConvertedArgumentMap?.get(valueArgument)

    private fun calculateExpectedTypeForConvertedArguments(
        arguments: Map<KotlinCallArgument, UnwrappedType>,
        substitutor: NewTypeSubstitutor?,
    ): Map<ValueArgument, UnwrappedType>? {
        if (arguments.isEmpty()) return null

        val expectedTypeForConvertedArguments = hashMapOf<ValueArgument, UnwrappedType>()
        for ((argument, convertedType) in arguments) {
            val typeWithFreshVariables = resolvedCallAtom.freshVariablesSubstitutor.safeSubstitute(convertedType)
            val expectedType = substitutor?.safeSubstitute(typeWithFreshVariables) ?: typeWithFreshVariables
            expectedTypeForConvertedArguments[argument.psiCallArgument.valueArgument] = expectedType
        }

        return expectedTypeForConvertedArguments
    }

    private fun calculateExpectedTypeForConstantConvertedArgumentMap() {
        if (resolvedCallAtom.argumentsWithConstantConversion.isEmpty()) return

        val expectedTypeForConvertedArguments = hashMapOf<KtExpression, IntegerValueTypeConstant>()

        for ((argument, convertedConstant) in resolvedCallAtom.argumentsWithConstantConversion) {
            val expression = argument.psiExpression ?: continue
            expectedTypeForConvertedArguments[expression] = convertedConstant
        }

        argumentTypeForConstantConvertedMap = expectedTypeForConvertedArguments
    }

    private fun calculateExpectedTypeForSamConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        expectedTypeForSamConvertedArgumentMap = calculateExpectedTypeForConvertedArguments(
            resolvedCallAtom.argumentsWithConversion.mapValues { it.value.convertedTypeByCandidateParameter },
            substitutor
        )
    }

    private fun calculateExpectedTypeForSuspendConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        expectedTypeForSuspendConvertedArgumentMap = calculateExpectedTypeForConvertedArguments(
            resolvedCallAtom.argumentsWithSuspendConversion, substitutor
        )
    }

    private fun calculateExpectedTypeForUnitConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        expectedTypeForUnitConvertedArgumentMap = calculateExpectedTypeForConvertedArguments(
            resolvedCallAtom.argumentsWithUnitConversion, substitutor
        )
    }

    private fun collectErrorPositions(): Map<ValueArgument, List<KotlinCallDiagnostic>> {
        val result = mutableListOf<Pair<ValueArgument, KotlinCallDiagnostic>>()

        fun ConstraintPosition.originalPosition(): ConstraintPosition =
            if (this is IncorporationConstraintPosition) {
                from.originalPosition()
            } else {
                this
            }

        diagnostics.forEach {
            val position = when (val error = it.constraintSystemError) {
                is NewConstraintError -> error.position.originalPosition()
                is CapturedTypeFromSubtyping -> error.position.originalPosition()
                is ConstrainingTypeIsError -> error.position.originalPosition()
                else -> null
            } as? ArgumentConstraintPositionImpl ?: return@forEach

            val argument = position.argument.safeAs<PSIKotlinCallArgument>()?.valueArgument ?: return@forEach
            result += argument to it
        }

        return result.groupBy({ it.first }) { it.second }
    }

    init {
        setResultingSubstitutor(substitutor)
    }
}
