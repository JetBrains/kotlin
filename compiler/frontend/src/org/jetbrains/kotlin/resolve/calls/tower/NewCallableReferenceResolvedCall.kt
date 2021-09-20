/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.inference.substituteAndApproximateTypes
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.util.isNotSimpleCall
import org.jetbrains.kotlin.resolve.calls.util.toResolutionStatus
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class NewCallableReferenceResolvedCall<D : CallableDescriptor>(
    val resolvedAtom: ResolvedCallableReferenceAtom,
    val typeApproximator: TypeApproximator,
    substitutor: NewTypeSubstitutor?,
) : NewAbstractResolvedCall<D>() {
    override var isCompleted = false
        private set

    override val resolvedCallAtom: MutableResolvedCallAtom? = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.resolvedCall
    }

    override val psiKotlinCall: PSIKotlinCall = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom.psiKotlinCall
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.atom.call.psiKotlinCall
    }

    init {
        setResultingSubstitutor(substitutor)
    }

    var diagnostics: Collection<KotlinCallDiagnostic> = mutableListOf()
    private lateinit var resultingDescriptor: D

    override fun getStatus(): ResolutionStatus {
        return getResultApplicability(diagnostics).toResolutionStatus()
    }

    override fun getCandidateDescriptor(): D = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.candidateDescriptor as D
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.candidate as D
    }

    override fun getResultingDescriptor(): D = resultingDescriptor

    private var extensionReceiver = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.extensionReceiverArgument?.receiverValue
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.extensionReceiver?.receiver?.receiverValue
    }

    private var dispatchReceiver = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.dispatchReceiverArgument?.receiverValue
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.dispatchReceiver?.receiver?.receiverValue
    }

    override fun getExtensionReceiver(): ReceiverValue? = extensionReceiver

    override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver

    override fun getExplicitReceiverKind(): ExplicitReceiverKind = when (resolvedAtom) {
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate!!.explicitReceiverKind
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.explicitReceiverKind
    }

    override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> = _valueArguments ?: emptyMap()

    override fun getValueArgumentsByIndex(): List<ResolvedValueArgument>? {
        val arguments = ArrayList<ResolvedValueArgument?>(candidateDescriptor.valueParameters.size)
        for (i in 0 until candidateDescriptor.valueParameters.size) {
            arguments.add(null)
        }

        for ((parameterDescriptor, value) in valueArguments) {
            val oldValue = arguments.set(parameterDescriptor.index, value)
            if (oldValue != null) {
                return null
            }
        }

        if (arguments.any { it == null }) return null

        @Suppress("UNCHECKED_CAST")
        return arguments as List<ResolvedValueArgument>
    }

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping = ArgumentUnmapped

    override fun getTypeArguments(): Map<TypeParameterDescriptor, KotlinType> {
        val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
        return typeParameters.zip(typeArguments).toMap()
    }

    private lateinit var typeArguments: List<UnwrappedType>

    override fun getDataFlowInfoForArguments(): DataFlowInfoForArguments =
        MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)

    override fun getSmartCastDispatchReceiverType(): KotlinType? = null


    private fun updateDispatchReceiverType(newType: KotlinType) {
        if (dispatchReceiver?.type == newType) return
        dispatchReceiver = dispatchReceiver?.replaceType(newType)
    }

    private fun updateExtensionReceiverType(newType: KotlinType) {
        if (extensionReceiver?.type == newType) return
        extensionReceiver = extensionReceiver?.replaceType(newType)
    }

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        //clear cached values
        if (substitutor != null) {
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

        val sub = when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.freshVariablesSubstitutor
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate!!.freshVariablesSubstitutor!!
        }

        typeArguments = sub.freshVariables.map {
            val substituted = (substitutor ?: FreshVariableNewTypeSubstitutor.Empty).safeSubstitute(it.defaultType)
            typeApproximator
                .approximateToSuperType(substituted, TypeApproximatorConfiguration.IntegerLiteralsTypesApproximation)
                ?: substituted
        }
    }

    private fun getSubstitutorWithoutFlexibleTypes(
        currentSubstitutor: NewTypeSubstitutor?,
        explicitTypeArguments: List<SimpleTypeArgument>,
    ): NewTypeSubstitutor? {
        if (currentSubstitutor !is NewTypeSubstitutorByConstructorMap || explicitTypeArguments.isEmpty()) return currentSubstitutor
        if (!currentSubstitutor.map.any { (_, value) -> value.isFlexible() }) return currentSubstitutor

        val sub = when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.freshVariablesSubstitutor
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate!!.freshVariablesSubstitutor!!
        }

        val typeVariables = sub.freshVariables
        val newSubstitutorMap = currentSubstitutor.map.toMutableMap()

        explicitTypeArguments.forEachIndexed { index, typeArgument ->
            val typeVariableConstructor = typeVariables.getOrNull(index)?.freshTypeConstructor ?: return@forEachIndexed

            newSubstitutorMap[typeVariableConstructor] =
                newSubstitutorMap[typeVariableConstructor]?.withNullabilityFromExplicitTypeArgument(typeArgument)
                    ?: return@forEachIndexed
        }

        return NewTypeSubstitutorByConstructorMap(newSubstitutorMap)
    }

    private fun KotlinType.withNullabilityFromExplicitTypeArgument(typeArgument: SimpleTypeArgument) =
        (if (typeArgument.type.isMarkedNullable) makeNullable() else makeNotNullable()).unwrap()

    private fun substitutedResultingDescriptor(substitutor: NewTypeSubstitutor?): CallableDescriptor {
        val candidateDescriptor = when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.candidateDescriptor
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate!!.candidate
        }

        return when (candidateDescriptor) {
            is ClassConstructorDescriptor, is SyntheticMemberDescriptor<*> -> {
                val explicitTypeArguments = emptyList<SimpleTypeArgument>()

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
    }

    private fun CallableDescriptor.substituteInferredVariablesAndApproximate(
        substitutor: NewTypeSubstitutor?,
        shouldApproximate: Boolean = true
    ): CallableDescriptor {
        val sub = when (resolvedAtom) {
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate!!.freshVariablesSubstitutor!!
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.freshVariablesSubstitutor
        }

        val inferredTypeVariablesSubstitutor = substitutor ?: FreshVariableNewTypeSubstitutor.Empty

        return substitute(sub).substituteAndApproximateTypes(
            inferredTypeVariablesSubstitutor,
            typeApproximator = if (shouldApproximate) typeApproximator else null,
            positionDependentApproximation = true
        )
    }

    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = TODO("Not yet implemented")

    override val kotlinCall: KotlinCall
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceArgumentAtom ->
                (resolvedAtom.candidate?.kotlinCall as CallableReferenceKotlinCallArgument).call
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom
        }
    override val languageVersionSettings: LanguageVersionSettings
        get() = TODO("Not yet implemented")

    override fun containsOnlyOnlyInputTypesErrors(): Boolean {
        TODO("Not yet implemented")
    }

    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ): Map<ValueArgument, ArgumentMatchImpl> {
        TODO("Not yet implemented")
    }
}