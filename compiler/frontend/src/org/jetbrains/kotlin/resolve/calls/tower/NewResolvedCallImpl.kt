/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.inference.substituteAndApproximateTypes
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.util.isNotSimpleCall
import org.jetbrains.kotlin.resolve.calls.util.toResolutionStatus
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.resolve.scopes.receivers.CastImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class NewResolvedCallImpl<D : CallableDescriptor>(
    val resolvedCallAtom: ResolvedCallAtom,
    substitutor: NewTypeSubstitutor?,
    private var diagnostics: Collection<KotlinCallDiagnostic>,
    private val typeApproximator: TypeApproximator,
    override val languageVersionSettings: LanguageVersionSettings,
) : NewAbstractResolvedCall<D>() {
    var isCompleted = false
        private set
    private lateinit var resultingDescriptor: D

    private lateinit var typeArguments: List<UnwrappedType>

    private var extensionReceiver = resolvedCallAtom.extensionReceiverArgument?.receiver?.receiverValue
    private var dispatchReceiver = resolvedCallAtom.dispatchReceiverArgument?.receiver?.receiverValue
    private var smartCastDispatchReceiverType: KotlinType? = null
    private var expectedTypeForSamConvertedArgumentMap: MutableMap<ValueArgument, UnwrappedType>? = null
    private var expectedTypeForSuspendConvertedArgumentMap: MutableMap<ValueArgument, UnwrappedType>? = null
    private var expectedTypeForUnitConvertedArgumentMap: MutableMap<ValueArgument, UnwrappedType>? = null
    private var argumentTypeForConstantConvertedMap: MutableMap<KtExpression, IntegerValueTypeConstant>? = null


    override val kotlinCall: KotlinCall get() = resolvedCallAtom.atom

    override fun getStatus(): ResolutionStatus = getResultApplicability(diagnostics).toResolutionStatus()

    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = resolvedCallAtom.argumentMappingByOriginal

    @Suppress("UNCHECKED_CAST")
    override fun getCandidateDescriptor(): D = resolvedCallAtom.candidateDescriptor as D
    override fun getResultingDescriptor(): D = resultingDescriptor
    override fun getExtensionReceiver(): ReceiverValue? = extensionReceiver
    override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver
    override fun getExplicitReceiverKind(): ExplicitReceiverKind = resolvedCallAtom.explicitReceiverKind

    override fun getTypeArguments(): Map<TypeParameterDescriptor, KotlinType> {
        val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
        return typeParameters.zip(typeArguments).toMap()
    }

    override fun containsOnlyOnlyInputTypesErrors() =
        diagnostics.all { it is KotlinConstraintSystemDiagnostic && it.error is OnlyInputTypesDiagnostic }

    override fun getSmartCastDispatchReceiverType(): KotlinType? = smartCastDispatchReceiverType

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

    private fun updateExtensionReceiverType(newType: KotlinType) {
        if (extensionReceiver?.type == newType) return
        extensionReceiver = extensionReceiver?.replaceType(newType)
    }

    private fun updateDispatchReceiverType(newType: KotlinType) {
        if (dispatchReceiver?.type == newType) return
        dispatchReceiver = dispatchReceiver?.replaceType(newType)
    }

    fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        //clear cached values
        argumentToParameterMap = null
        _valueArguments = null
        if (substitutor != null) {
            // todo: add asset that we do not complete call many times
            isCompleted = true

            dispatchReceiver?.type?.let {
                val newType = substitutor.safeSubstitute(it.unwrap())
                updateDispatchReceiverType(newType)
            }

            extensionReceiver?.type?.let {
                val newType = substitutor.safeSubstitute(it.unwrap())
                updateExtensionReceiverType(newType)
            }
        }

        @Suppress("UNCHECKED_CAST")
        resultingDescriptor = substitutedResultingDescriptor(substitutor) as D

        typeArguments = resolvedCallAtom.freshVariablesSubstitutor.freshVariables.map {
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

    private fun KotlinType.withNullabilityFromExplicitTypeArgument(typeArgument: SimpleTypeArgument) =
        (if (typeArgument.type.isMarkedNullable) makeNullable() else makeNotNullable()).unwrap()

    private fun getSubstitutorWithoutFlexibleTypes(
        currentSubstitutor: NewTypeSubstitutor?,
        explicitTypeArguments: List<SimpleTypeArgument>,
    ): NewTypeSubstitutor? {
        if (currentSubstitutor !is NewTypeSubstitutorByConstructorMap || explicitTypeArguments.isEmpty()) return currentSubstitutor
        if (!currentSubstitutor.map.any { (_, value) -> value.isFlexible() }) return currentSubstitutor

        val typeVariables = resolvedCallAtom.freshVariablesSubstitutor.freshVariables
        val newSubstitutorMap = currentSubstitutor.map.toMutableMap()

        explicitTypeArguments.forEachIndexed { index, typeArgument ->
            val typeVariableConstructor = typeVariables.getOrNull(index)?.freshTypeConstructor ?: return@forEachIndexed

            newSubstitutorMap[typeVariableConstructor] =
                newSubstitutorMap[typeVariableConstructor]?.withNullabilityFromExplicitTypeArgument(typeArgument)
                    ?: return@forEachIndexed
        }

        return NewTypeSubstitutorByConstructorMap(newSubstitutorMap)
    }

    private fun substitutedResultingDescriptor(substitutor: NewTypeSubstitutor?) =
        when (val candidateDescriptor = resolvedCallAtom.candidateDescriptor) {
            is ClassConstructorDescriptor, is SyntheticMemberDescriptor<*> -> {
                val explicitTypeArguments = resolvedCallAtom.atom.typeArguments.filterIsInstance<SimpleTypeArgument>()

                candidateDescriptor.substituteInferredVariablesAndApproximate(
                    getSubstitutorWithoutFlexibleTypes(substitutor, explicitTypeArguments),
                )
            }
            is FunctionDescriptor -> {
                candidateDescriptor.substituteInferredVariablesAndApproximate(substitutor, candidateDescriptor.isNotSimpleCall())
            }
            is PropertyDescriptor -> {
                if (candidateDescriptor.isNotSimpleCall()) {
                    candidateDescriptor.substituteInferredVariablesAndApproximate(substitutor)
                } else {
                    candidateDescriptor
                }
            }
            else -> candidateDescriptor
        }

    private fun CallableDescriptor.substituteInferredVariablesAndApproximate(
        substitutor: NewTypeSubstitutor?,
        shouldApproximate: Boolean = true
    ): CallableDescriptor {
        val inferredTypeVariablesSubstitutor = substitutor ?: FreshVariableNewTypeSubstitutor.Empty

        // TODO: merge last two substitutors to avoid redundant descriptor substitutions
        return substitute(resolvedCallAtom.freshVariablesSubstitutor).substitute(resolvedCallAtom.knownParametersSubstitutor)
            .substituteAndApproximateTypes(inferredTypeVariablesSubstitutor, if (shouldApproximate) typeApproximator else null)
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

    private fun calculateExpectedTypeForConstantConvertedArgumentMap() {
        if (resolvedCallAtom.argumentsWithConstantConversion.isEmpty()) return

        argumentTypeForConstantConvertedMap = hashMapOf()
        for ((argument, convertedConstant) in resolvedCallAtom.argumentsWithConstantConversion) {
            val expression = argument.psiExpression ?: continue
            argumentTypeForConstantConvertedMap!![expression] = convertedConstant
        }
    }

    private fun calculateExpectedTypeForSamConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        if (resolvedCallAtom.argumentsWithConversion.isEmpty()) return

        expectedTypeForSamConvertedArgumentMap = hashMapOf()
        for ((argument, description) in resolvedCallAtom.argumentsWithConversion) {
            val typeWithFreshVariables =
                resolvedCallAtom.freshVariablesSubstitutor.safeSubstitute(description.convertedTypeByCandidateParameter)
            val expectedType = substitutor?.safeSubstitute(typeWithFreshVariables) ?: typeWithFreshVariables
            expectedTypeForSamConvertedArgumentMap!![argument.psiCallArgument.valueArgument] = expectedType
        }
    }

    private fun calculateExpectedTypeForSuspendConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        if (resolvedCallAtom.argumentsWithSuspendConversion.isEmpty()) return

        expectedTypeForSuspendConvertedArgumentMap = hashMapOf()
        for ((argument, convertedType) in resolvedCallAtom.argumentsWithSuspendConversion) {
            val typeWithFreshVariables = resolvedCallAtom.freshVariablesSubstitutor.safeSubstitute(convertedType)
            val expectedType = substitutor?.safeSubstitute(typeWithFreshVariables) ?: typeWithFreshVariables
            expectedTypeForSuspendConvertedArgumentMap!![argument.psiCallArgument.valueArgument] = expectedType
        }
    }

    private fun calculateExpectedTypeForUnitConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        if (resolvedCallAtom.argumentsWithUnitConversion.isEmpty()) return

        expectedTypeForUnitConvertedArgumentMap = hashMapOf()
        for ((argument, convertedType) in resolvedCallAtom.argumentsWithUnitConversion) {
            val typeWithFreshVariables = resolvedCallAtom.freshVariablesSubstitutor.safeSubstitute(convertedType)
            val expectedType = substitutor?.safeSubstitute(typeWithFreshVariables) ?: typeWithFreshVariables
            expectedTypeForUnitConvertedArgumentMap!![argument.psiCallArgument.valueArgument] = expectedType
        }
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
