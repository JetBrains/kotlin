/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.results

import gnu.trove.THashSet
import gnu.trove.TObjectHashingStrategy
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.*
import org.jetbrains.kotlin.resolve.calls.inference.toHandle
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionMutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.check

class OverloadingConflictResolver(private val builtIns: KotlinBuiltIns) {

    fun <D : CallableDescriptor> findMaximallySpecific(
            candidates: Set<MutableResolvedCall<D>>,
            checkArgumentsMode: CheckArgumentTypesMode,
            discriminateGenerics: Boolean
    ): MutableResolvedCall<D>? =
            if (candidates.size <= 1)
                candidates.firstOrNull()
            else when (checkArgumentsMode) {
                CheckArgumentTypesMode.CHECK_CALLABLE_TYPE ->
                    uniquifyCandidatesSet(candidates).filter {
                        isDefinitelyMostSpecific(it, candidates) {
                            call1, call2 ->
                            isNotLessSpecificCallableReference(call1.resultingDescriptor, call2.resultingDescriptor)
                        }
                    }.singleOrNull()

                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS ->
                    findMaximallySpecificCall(candidates, discriminateGenerics)
            }

    fun <D : CallableDescriptor> findMaximallySpecificVariableAsFunctionCalls(candidates: Set<MutableResolvedCall<D>>): Set<MutableResolvedCall<D>> {
        val variableCalls = candidates.mapTo(newResolvedCallSet<MutableResolvedCall<VariableDescriptor>>(candidates.size)) {
            if (it is VariableAsFunctionMutableResolvedCall)
                it.variableCall
            else
                throw AssertionError("Regular call among variable-as-function calls: $it")
        }

        val maxSpecificVariableCall = findMaximallySpecificCall(variableCalls, false) ?: return emptySet()

        return candidates.filterTo(newResolvedCallSet<MutableResolvedCall<D>>(2)) {
            it.resultingVariableDescriptor == maxSpecificVariableCall.resultingDescriptor
        }
    }

    private fun <D : CallableDescriptor> findMaximallySpecificCall(
            candidates: Set<MutableResolvedCall<D>>,
            discriminateGenerics: Boolean
    ): MutableResolvedCall<D>? {
        val filteredCandidates = uniquifyCandidatesSet(candidates)

        if (filteredCandidates.size <= 1) return filteredCandidates.singleOrNull()

        val conflictingCandidates = filteredCandidates.map {
            candidateCall ->
            CandidateCallWithArgumentMapping.create(candidateCall) { it.arguments.filter { it.getArgumentExpression() != null } }
        }

        val bestCandidatesByParameterTypes = conflictingCandidates.mapNotNull {
            candidate ->
            candidate.check {
                isMostSpecific(candidate, conflictingCandidates) {
                    call1, call2 ->
                    isNotLessSpecificCallWithArgumentMapping(call1, call2, discriminateGenerics)
                }
            }
        }

        return bestCandidatesByParameterTypes.exactMaxWith { call1, call2 -> isOfNotLessSpecificShape(call1, call2) }?.resolvedCall
    }

    private inline fun <C : Any> Collection<C>.exactMaxWith(isNotWorse: (C, C) -> Boolean): C? {
        var result: C? = null
        for (candidate in this) {
            if (result == null || isNotWorse(candidate, result)) {
                result = candidate
            }
        }
        if (result == null) return null
        if (any { it != result && isNotWorse(it, result!!) }) {
            return null
        }
        return result
    }

    private inline fun <C> isMostSpecific(candidate: C, candidates: Collection<C>, isNotLessSpecific: (C, C) -> Boolean): Boolean =
            candidates.all {
                other ->
                candidate === other ||
                isNotLessSpecific(candidate, other)
            }

    private inline fun <C> isDefinitelyMostSpecific(candidate: C, candidates: Collection<C>, isNotLessSpecific: (C, C) -> Boolean): Boolean =
            candidates.all {
                other ->
                candidate === other ||
                isNotLessSpecific(candidate, other) && !isNotLessSpecific(other, candidate)
            }

    /**
     * `call1` is not less specific than `call2`
     */
    private fun <D : CallableDescriptor, K> isNotLessSpecificCallWithArgumentMapping(
            call1: CandidateCallWithArgumentMapping<D, K>,
            call2: CandidateCallWithArgumentMapping<D, K>,
            discriminateGenerics: Boolean
    ): Boolean {
        return tryCompareDescriptorsFromScripts(call1.resultingDescriptor, call2.resultingDescriptor) ?:
               compareCallsByUsedArguments(call1, call2, discriminateGenerics)
    }

    /**
     * Returns `true` if `d1` is definitely not less specific than `d2`,
     * `false` otherwise.
     */
    private fun <D : CallableDescriptor, K> compareCallsByUsedArguments(
            call1: CandidateCallWithArgumentMapping<D, K>,
            call2: CandidateCallWithArgumentMapping<D, K>,
            discriminateGenerics: Boolean
    ): Boolean {
        if (discriminateGenerics) {
            val isGeneric1 = call1.isGeneric
            val isGeneric2 = call2.isGeneric
            // generic loses to non-generic
            if (isGeneric1 && !isGeneric2) return false
            if (!isGeneric1 && isGeneric2) return true
            // two generics are non-comparable
            if (isGeneric1 && isGeneric2) return false
        }

        val typeParameters = call2.resolvedCall.resultingDescriptor.typeParameters
        val constraintSystemBuilder: ConstraintSystem.Builder = ConstraintSystemBuilderImpl()
        var hasConstraints = false
        val typeSubstitutor = constraintSystemBuilder.registerTypeVariables(call1.resolvedCall.call.toHandle(), typeParameters)

        fun compareTypesAndUpdateConstraints(type1: KotlinType?, type2: KotlinType?, constraintPosition: ConstraintPosition): Boolean {
            if (type1 == null || type2 == null) return true

            if (typeParameters.isEmpty() || !TypeUtils.dependsOnTypeParameters(type2, typeParameters)) {
                if (!typeNotLessSpecific(type1, type2)) {
                    return false
                }
            }
            else if (isDefinitelyLessSpecificByTypeSpecificity(type1, type2)) {
                return false
            }
            else {
                hasConstraints = true
                val substitutedType2 = typeSubstitutor.safeSubstitute(type2, Variance.INVARIANT)
                constraintSystemBuilder.addSubtypeConstraint(type1, substitutedType2, constraintPosition)
            }
            return true
        }

        val extensionReceiverType1 = call1.extensionReceiverType
        val extensionReceiverType2 = call2.extensionReceiverType
        if (!compareTypesAndUpdateConstraints(extensionReceiverType1, extensionReceiverType2, RECEIVER_POSITION.position())) {
            return false
        }

        assert(call1.argumentsCount == call2.argumentsCount) {
            "$call1 and $call2 have different number of explicit arguments"
        }

        for (argumentKey in call1.argumentKeys) {
            val type1 = call1.getValueParameterType(argumentKey)
            val type2 = call2.getValueParameterType(argumentKey)

            // We use this constraint system for subtyping relation check only,
            // so exact value parameter position doesn't matter.
            if (!compareTypesAndUpdateConstraints(type1, type2, VALUE_PARAMETER_POSITION.position(0))) {
                return false
            }
        }

        if (hasConstraints) {
            constraintSystemBuilder.fixVariables()
            val constraintSystem = constraintSystemBuilder.build()
            if (constraintSystem.status.hasContradiction()) {
                return false
            }
        }

        return true
    }

    private fun <D: CallableDescriptor, K> isOfNotLessSpecificShape(
            call1: CandidateCallWithArgumentMapping<D, K>,
            call2: CandidateCallWithArgumentMapping<D, K>
    ): Boolean {
        val hasVarargs1 = call1.resultingDescriptor.hasVarargs
        val hasVarargs2 = call2.resultingDescriptor.hasVarargs
        if (hasVarargs1 && !hasVarargs2) return false
        if (!hasVarargs1 && hasVarargs2) return true

        if (call1.parametersWithDefaultValuesCount > call2.parametersWithDefaultValuesCount) {
            return false
        }

        return true
    }

    /**
     * Returns `true` if `d1` is definitely not less specific than `d2`,
     * `false` if `d1` is definitely less specific than `d2`,
     * `null` if undecided.
     */
    private fun tryCompareDescriptorsFromScripts(d1: CallableDescriptor, d2: CallableDescriptor): Boolean? {
        val containingDeclaration1 = d1.containingDeclaration
        val containingDeclaration2 = d2.containingDeclaration

        if (containingDeclaration1 is ScriptDescriptor && containingDeclaration2 is ScriptDescriptor) {
            when {
                containingDeclaration1.priority > containingDeclaration2.priority -> return true
                containingDeclaration1.priority < containingDeclaration2.priority -> return false
            }
        }
        return null
    }

    /**
     * Returns `false` if `d1` is definitely less specific than `d2`,
     * `null` if undecided.
     */
    private fun tryCompareExtensionReceiverType(type1: KotlinType?, type2: KotlinType?): Boolean? {
        if (type1 != null && type2 != null) {
            if (!typeNotLessSpecific(type1, type2))
                return false
        }
        return null
    }

    /**
     * Returns `true` if `d1` is definitely not less specific than `d2`,
     * `false` if `d1` is definitely less specific than `d2`,
     * `null` if undecided.
     */
    private fun compareFunctionParameterTypes(f: CallableDescriptor, g: CallableDescriptor): Boolean {
        val fParams = f.valueParameters
        val gParams = g.valueParameters

        val fSize = fParams.size
        val gSize = gParams.size

        if (fSize != gSize) return false

        for (i in 0..fSize - 1) {
            val fParam = fParams[i]
            val gParam = gParams[i]

            val fParamIsVararg = fParam.varargElementType != null
            val gParamIsVararg = gParam.varargElementType != null

            if (fParamIsVararg != gParamIsVararg) {
                return false
            }

            val fParamType = getVarargElementTypeOrType(fParam)
            val gParamType = getVarargElementTypeOrType(gParam)

            if (!typeNotLessSpecific(fParamType, gParamType)) {
                return false
            }
        }

        return true
    }

    private fun isNotLessSpecificCallableReference(f: CallableDescriptor, g: CallableDescriptor): Boolean =
            // TODO should we "discriminate generic descriptors" for callable references?
            tryCompareDescriptorsFromScripts(f, g) ?:
            // TODO compare functional types for 'f' and 'g'
            tryCompareExtensionReceiverType(f.extensionReceiverType, g.extensionReceiverType) ?:
            compareFunctionParameterTypes(f, g)

    private fun getVarargElementTypeOrType(parameterDescriptor: ValueParameterDescriptor): KotlinType =
            parameterDescriptor.varargElementType ?: parameterDescriptor.type

    private val CallableDescriptor.hasVarargs: Boolean get() =
            this.valueParameters.any { it.varargElementType != null }

    private fun typeNotLessSpecific(specific: KotlinType, general: KotlinType): Boolean {
        val isSubtype = KotlinTypeChecker.DEFAULT.isSubtypeOf(specific, general) || numericTypeMoreSpecific(specific, general)

        if (!isSubtype) return false

        if (isDefinitelyLessSpecificByTypeSpecificity(specific, general)) return false

        return true
    }

    private fun isDefinitelyLessSpecificByTypeSpecificity(specific: KotlinType, general: KotlinType): Boolean {
        val sThanG = specific.getSpecificityRelationTo(general)
        val gThanS = general.getSpecificityRelationTo(specific)
        return sThanG == Specificity.Relation.LESS_SPECIFIC &&
               gThanS != Specificity.Relation.LESS_SPECIFIC
    }

    private fun numericTypeMoreSpecific(specific: KotlinType, general: KotlinType): Boolean {
        val _double = builtIns.doubleType
        val _float = builtIns.floatType
        val _long = builtIns.longType
        val _int = builtIns.intType
        val _byte = builtIns.byteType
        val _short = builtIns.shortType

        when {
            TypeUtils.equalTypes(specific, _double) && TypeUtils.equalTypes(general, _float) -> return true
            TypeUtils.equalTypes(specific, _int) -> {
                when {
                    TypeUtils.equalTypes(general, _long) -> return true
                    TypeUtils.equalTypes(general, _byte) -> return true
                    TypeUtils.equalTypes(general, _short) -> return true
                }
            }
            TypeUtils.equalTypes(specific, _short) && TypeUtils.equalTypes(general, _byte) -> return true
        }

        return false
    }

    private val CallableDescriptor.extensionReceiverType: KotlinType?
        get() = extensionReceiverParameter?.type

    companion object {
        // Different smartcasts may lead to the same candidate descriptor wrapped into different ResolvedCallImpl objects
        @Suppress("CAST_NEVER_SUCCEEDS")
        private fun <D : CallableDescriptor> uniquifyCandidatesSet(candidates: Collection<MutableResolvedCall<D>>): Set<MutableResolvedCall<D>> =
                THashSet<MutableResolvedCall<D>>(candidates.size, getCallHashingStrategy<MutableResolvedCall<D>>()).apply { addAll(candidates) }

        @Suppress("CAST_NEVER_SUCCEEDS")
        private fun <C> newResolvedCallSet(expectedSize: Int): MutableSet<C> =
                THashSet<C>(expectedSize, getCallHashingStrategy<C>())

        private object ResolvedCallHashingStrategy : TObjectHashingStrategy<ResolvedCall<*>> {
            override fun equals(call1: ResolvedCall<*>?, call2: ResolvedCall<*>?): Boolean =
                    if (call1 != null && call2 != null)
                        call1.resultingDescriptor == call2.resultingDescriptor
                    else
                        call1 == call2

            override fun computeHashCode(call: ResolvedCall<*>?): Int =
                    call?.resultingDescriptor?.hashCode() ?: 0
        }

        private val MutableResolvedCall<*>.resultingVariableDescriptor: VariableDescriptor
            get() = (this as VariableAsFunctionResolvedCall).variableCall.resultingDescriptor

        @Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")
        private fun <C> getCallHashingStrategy() =
                ResolvedCallHashingStrategy as TObjectHashingStrategy<C>

    }
    
}
