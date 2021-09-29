/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.inference.substituteAndApproximateTypes
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.util.isNotSimpleCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.compactIfPossible

sealed class NewAbstractResolvedCall<D : CallableDescriptor> : ResolvedCall<D> {
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    abstract val kotlinCall: KotlinCall?
    abstract val languageVersionSettings: LanguageVersionSettings
    abstract val resolvedCallAtom: ResolvedCallAtom?
    abstract val psiKotlinCall: PSIKotlinCall
    abstract val typeApproximator: TypeApproximator
    abstract val freshSubstitutor: FreshVariableNewTypeSubstitutor?

    protected open val positionDependentApproximation = false

    private var argumentToParameterMap: Map<ValueArgument, ArgumentMatchImpl>? = null
    private var valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>? = null
    private var nonTrivialUpdatedResultInfo: DataFlowInfo? = null
    private var isCompleted: Boolean = false

    abstract fun updateDispatchReceiverType(newType: KotlinType)
    abstract fun updateExtensionReceiverType(newType: KotlinType)
    abstract fun updateContextReceiverTypes(newTypes: List<KotlinType>)
    abstract fun containsOnlyOnlyInputTypesErrors(): Boolean
    abstract fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?)
    abstract fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>,
    ): Map<ValueArgument, ArgumentMatchImpl>

    override fun getCall(): Call = psiKotlinCall.psiCall

    override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> {
        if (valueArguments == null) {
            valueArguments = createValueArguments()
        }
        return valueArguments!!
    }

    override fun getValueArgumentsByIndex(): List<ResolvedValueArgument>? {
        val arguments = ArrayList<ResolvedValueArgument?>(candidateDescriptor.valueParameters.size)
        for (i in 0 until candidateDescriptor.valueParameters.size) {
            arguments.add(null)
        }

        for ((parameterDescriptor, value) in getValueArguments()) {
            val oldValue = arguments.set(parameterDescriptor.index, value)
            if (oldValue != null) {
                return null
            }
        }

        if (arguments.any { it == null }) return null

        @Suppress("UNCHECKED_CAST")
        return arguments as List<ResolvedValueArgument>
    }

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping {
        if (argumentToParameterMap == null) {
            updateArgumentsMapping(argumentToParameterMap(resultingDescriptor, getValueArguments()))
        }
        return argumentToParameterMap!![valueArgument] ?: ArgumentUnmapped
    }

    override fun getDataFlowInfoForArguments() = object : DataFlowInfoForArguments {
        override fun getResultInfo(): DataFlowInfo = nonTrivialUpdatedResultInfo ?: psiKotlinCall.resultDataFlowInfo

        override fun getInfo(valueArgument: ValueArgument): DataFlowInfo {
            val externalPsiCallArgument = kotlinCall?.externalArgument?.psiCallArgument
            if (externalPsiCallArgument?.valueArgument == valueArgument) {
                return externalPsiCallArgument.dataFlowInfoAfterThisArgument
            }
            return psiKotlinCall.dataFlowInfoForArguments.getInfo(valueArgument)
        }
    }

    fun updateValueArguments(newValueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>?) {
        valueArguments = newValueArguments
    }

    fun substituteReceivers(substitutor: NewTypeSubstitutor?) {
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
    }

    fun isCompleted() = isCompleted

    // Currently, updated only with info from effect system
    internal fun updateResultingDataFlowInfo(dataFlowInfo: DataFlowInfo) {
        if (dataFlowInfo == DataFlowInfo.EMPTY) return
        assert(nonTrivialUpdatedResultInfo == null) {
            "Attempt to rewrite resulting dataFlowInfo enhancement for call: $kotlinCall"
        }
        nonTrivialUpdatedResultInfo = dataFlowInfo.and(psiKotlinCall.resultDataFlowInfo)
    }

    protected fun updateArgumentsMapping(newMapping: Map<ValueArgument, ArgumentMatchImpl>?) {
        argumentToParameterMap = newMapping
    }

    protected fun substitutedResultingDescriptor(substitutor: NewTypeSubstitutor?) =
        when (val candidateDescriptor = candidateDescriptor) {
            is ClassConstructorDescriptor, is SyntheticMemberDescriptor<*> -> {
                val explicitTypeArguments = resolvedCallAtom?.atom?.typeArguments?.filterIsInstance<SimpleTypeArgument>() ?: emptyList()

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

        val freshVariablesSubstituted = freshSubstitutor?.let(::substitute) ?: this
        val knownTypeParameterSubstituted = resolvedCallAtom?.knownParametersSubstitutor?.let(freshVariablesSubstituted::substitute)
            ?: freshVariablesSubstituted

        return knownTypeParameterSubstituted.substituteAndApproximateTypes(
            inferredTypeVariablesSubstitutor,
            typeApproximator = if (shouldApproximate) typeApproximator else null,
            positionDependentApproximation
        )
    }

    private fun getSubstitutorWithoutFlexibleTypes(
        currentSubstitutor: NewTypeSubstitutor?,
        explicitTypeArguments: List<SimpleTypeArgument>,
    ): NewTypeSubstitutor? {
        if (currentSubstitutor !is NewTypeSubstitutorByConstructorMap || explicitTypeArguments.isEmpty()) return currentSubstitutor
        if (!currentSubstitutor.map.any { (_, value) -> value.isFlexible() }) return currentSubstitutor

        val typeVariables = freshSubstitutor?.freshVariables ?: return null
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

    private fun createValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> =
        LinkedHashMap<ValueParameterDescriptor, ResolvedValueArgument>().also { result ->
            val needToUseCorrectExecutionOrderForVarargArguments =
                languageVersionSettings.supportsFeature(LanguageFeature.UseCorrectExecutionOrderForVarargArguments)
            var varargMappings: MutableList<Pair<ValueParameterDescriptor, VarargValueArgument>>? = null
            for ((originalParameter, resolvedCallArgument) in argumentMappingByOriginal) {
                val resultingParameter = resultingDescriptor.valueParameters[originalParameter.index]

                result[resultingParameter] = when (resolvedCallArgument) {
                    ResolvedCallArgument.DefaultArgument ->
                        DefaultValueArgument.DEFAULT
                    is ResolvedCallArgument.SimpleArgument -> {
                        val valueArgument = resolvedCallArgument.callArgument.psiCallArgument.valueArgument
                        if (resultingParameter.isVararg) {
                            if (needToUseCorrectExecutionOrderForVarargArguments) {
                                VarargValueArgument().apply { addArgument(valueArgument) }
                            } else {
                                val vararg = VarargValueArgument().apply { addArgument(valueArgument) }
                                if (varargMappings == null) varargMappings = SmartList()
                                varargMappings.add(resultingParameter to vararg)
                                continue
                            }
                        } else {
                            ExpressionValueArgument(valueArgument)
                        }
                    }
                    is ResolvedCallArgument.VarargArgument ->
                        VarargValueArgument().apply {
                            resolvedCallArgument.arguments.map { it.psiCallArgument.valueArgument }.forEach { addArgument(it) }
                        }
                }
            }

            if (varargMappings != null && !needToUseCorrectExecutionOrderForVarargArguments) {
                for ((parameter, argument) in varargMappings) {
                    result[parameter] = argument
                }
            }
        }.compactIfPossible()
}