/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.descriptorUtil.varargParameterPosition
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import java.util.*

class OverloadingConflictResolver<C : Any>(
        private val builtIns: KotlinBuiltIns,
        private val specificityComparator: TypeSpecificityComparator,
        private val getResultingDescriptor: (C) -> CallableDescriptor,
        private val createEmptyConstraintSystem: () -> SimpleConstraintSystem,
        private val createFlatSignature: (C) -> FlatSignature<C>,
        private val getVariableCandidates: (C) -> C?, // vor variable WithInvoke
        private val isFromSources: (CallableDescriptor) -> Boolean
) {

    private val resolvedCallHashingStrategy = object : TObjectHashingStrategy<C> {
        override fun equals(call1: C?, call2: C?): Boolean =
                if (call1 != null && call2 != null)
                    call1.resultingDescriptor == call2.resultingDescriptor
                else
                    call1 == call2

        override fun computeHashCode(call: C?): Int =
                call?.resultingDescriptor?.hashCode() ?: 0
    }

    private val C.resultingDescriptor: CallableDescriptor get() = getResultingDescriptor(this)

    // if result contains only one element -- it is maximally specific; otherwise we have ambiguity
    fun chooseMaximallySpecificCandidates(
            candidates: Collection<C>,
            checkArgumentsMode: CheckArgumentTypesMode,
            discriminateGenerics: Boolean,
            isDebuggerContext: Boolean
    ): Set<C> {
        candidates.setIfOneOrEmpty()?.let { return it }

        val fixedCandidates = if (getVariableCandidates(candidates.first()) != null) {
            findMaximallySpecificVariableAsFunctionCalls(candidates, isDebuggerContext) ?: return LinkedHashSet(candidates)
        }
        else {
            candidates
        }

        val noEquivalentCalls = filterOutEquivalentCalls(fixedCandidates)
        val noOverrides = OverridingUtil.filterOverrides(noEquivalentCalls) { a, b ->
            val aDescriptor = a.resultingDescriptor
            val bDescriptor = b.resultingDescriptor
            // Here we'd like to handle situation when we have two synthetic descriptors as in syntheticSAMExtensions.kt

            // Without this, we'll pick all synthetic descriptors as they don't have overridden descriptors and
            // then report ambiguity, which isn't very convenient
            if (aDescriptor is SyntheticMemberDescriptor<*> && bDescriptor is SyntheticMemberDescriptor<*>) {
                val aBaseDescriptor = aDescriptor.baseDescriptorForSynthetic
                val bBaseDescriptor = bDescriptor.baseDescriptorForSynthetic
                if (aBaseDescriptor is CallableMemberDescriptor && bBaseDescriptor is CallableMemberDescriptor) {
                    return@filterOverrides Pair(aBaseDescriptor, bBaseDescriptor)
                }
            }
            Pair(aDescriptor, bDescriptor)
        }
        if (noOverrides.size == 1) {
            return noOverrides
        }

        val maximallySpecific = findMaximallySpecific(noOverrides, checkArgumentsMode, false, isDebuggerContext)
        if (maximallySpecific != null) {
            return setOf(maximallySpecific)
        }

        if (discriminateGenerics) {
            val maximallySpecificGenericsDiscriminated = findMaximallySpecific(noOverrides, checkArgumentsMode, true, isDebuggerContext)
            if (maximallySpecificGenericsDiscriminated != null) {
                return setOf(maximallySpecificGenericsDiscriminated)
            }
        }

        return noOverrides
    }

    // Sometimes we should compare "copies" from sources and from binary files.
    // But we cannot compare return types for such copies, because it may lead us to recursive problem (see KT-11995).
    // Because of this we compare them without return type and choose descriptor from source if we found duplicate.
    fun filterOutEquivalentCalls(candidates: Collection<C>): Set<C> {
        candidates.setIfOneOrEmpty()?.let { return it }

        val fromSourcesGoesFirst = candidates.sortedBy { if (isFromSources(it.resultingDescriptor)) 0 else 1 }

        val result = LinkedHashSet<C>()
        outerLoop@ for (meD in fromSourcesGoesFirst) {
            for (otherD in result) {
                val me = meD.resultingDescriptor
                val other = otherD.resultingDescriptor
                val ignoreReturnType = isFromSources(me) != isFromSources(other)
                if (DescriptorEquivalenceForOverrides.areCallableDescriptorsEquivalent(me, other, ignoreReturnType)) {
                    continue@outerLoop
                }
            }
            result.add(meD)
        }

        return result
    }

    private fun Collection<C>.setIfOneOrEmpty() = when(size) {
        0 -> emptySet()
        1 -> setOf(single())
        else -> null
    }

    private fun findMaximallySpecific(
            candidates: Set<C>,
            checkArgumentsMode: CheckArgumentTypesMode,
            discriminateGenerics: Boolean,
            isDebuggerContext: Boolean
    ): C? =
            if (candidates.size <= 1)
                candidates.firstOrNull()
            else when (checkArgumentsMode) {
                CheckArgumentTypesMode.CHECK_CALLABLE_TYPE ->
                    uniquifyCandidatesSet(candidates).singleOrNull {
                        isDefinitelyMostSpecific(it, candidates) { call1, call2 ->
                            isNotLessSpecificCallableReference(call1.resultingDescriptor, call2.resultingDescriptor)
                        }
                    }

                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS ->
                    findMaximallySpecificCall(candidates, discriminateGenerics, isDebuggerContext)
                    ?: findMaximallySpecificCall(
                            candidates.filterNotTo(mutableSetOf()) { createFlatSignature(it).isSyntheticMember },
                            discriminateGenerics, isDebuggerContext
                    )
            }

    // null means ambiguity between variables
    private fun findMaximallySpecificVariableAsFunctionCalls(candidates: Collection<C>, isDebuggerContext: Boolean): Set<C>? {
        val variableCalls = candidates.mapTo(newResolvedCallSet(candidates.size)) {
            getVariableCandidates(it) ?: throw AssertionError("Regular call among variable-as-function calls: $it")
        }

        val maxSpecificVariableCalls = chooseMaximallySpecificCandidates(variableCalls, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                                                                        isDebuggerContext = isDebuggerContext, discriminateGenerics = false)

        val maxSpecificVariableCall = maxSpecificVariableCalls.singleOrNull() ?: return null
        return candidates.filterTo(newResolvedCallSet(2)) {
            getVariableCandidates(it)!!.resultingDescriptor == maxSpecificVariableCall.resultingDescriptor
        }
    }

    private fun findMaximallySpecificCall(
            candidates: Set<C>,
            discriminateGenerics: Boolean,
            isDebuggerContext: Boolean
    ): C? {
        val filteredCandidates = uniquifyCandidatesSet(candidates)

        if (filteredCandidates.size <= 1) return filteredCandidates.singleOrNull()

        val conflictingCandidates = filteredCandidates.map {
            candidateCall ->
            createFlatSignature(candidateCall)
        }

        val bestCandidatesByParameterTypes = conflictingCandidates.filter {
            candidate ->
            isMostSpecific(candidate, conflictingCandidates) {
                call1, call2 ->
                isNotLessSpecificCallWithArgumentMapping(call1, call2, discriminateGenerics)
            }
        }

        return bestCandidatesByParameterTypes.exactMaxWith {
            call1, call2 ->
            isOfNotLessSpecificShape(call1, call2) && isOfNotLessSpecificVisibilityForDebugger(call1, call2, isDebuggerContext)
        }?.origin
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
    private fun isNotLessSpecificCallWithArgumentMapping(
            call1: FlatSignature<C>,
            call2: FlatSignature<C>,
            discriminateGenerics: Boolean
    ): Boolean {
        return tryCompareDescriptorsFromScripts(call1.candidateDescriptor(), call2.candidateDescriptor()) ?:
               compareCallsByUsedArguments(call1, call2, discriminateGenerics)
    }

    /**
     * Returns `true` if [call1] is definitely more or equally specific [call2],
     * `false` otherwise.
     */
    private fun compareCallsByUsedArguments(
            call1: FlatSignature<C>,
            call2: FlatSignature<C>,
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

        if (!call1.isHeader && call2.isHeader) return true
        if (call1.isHeader && !call2.isHeader) return false

        return createEmptyConstraintSystem().isSignatureNotLessSpecific(call1, call2, SpecificityComparisonWithNumerics, specificityComparator)
    }

    private val SpecificityComparisonWithNumerics = object : SpecificityComparisonCallbacks {
        override fun isNonSubtypeNotLessSpecific(specific: KotlinType, general: KotlinType): Boolean {
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
    }

    private fun isOfNotLessSpecificShape(
            call1: FlatSignature<C>,
            call2: FlatSignature<C>
    ): Boolean {
        val hasVarargs1 = call1.hasVarargs
        val hasVarargs2 = call2.hasVarargs
        if (hasVarargs1 && !hasVarargs2) return false
        if (!hasVarargs1 && hasVarargs2) return true

        if (call1.numDefaults > call2.numDefaults) {
            return false
        }

        return true
    }

    private fun isOfNotLessSpecificVisibilityForDebugger(
            call1: FlatSignature<C>,
            call2: FlatSignature<C>,
            isDebuggerContext: Boolean
    ): Boolean {
        if (isDebuggerContext) {
            val isMoreVisible1 = Visibilities.compare(call1.descriptorVisibility(), call2.descriptorVisibility())
            if (isMoreVisible1 != null && isMoreVisible1 < 0) return false
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
     * Returns `true` if `f` is definitely not less specific than `g`,
     * `false` if `f` is definitely less specific than `g`,
     * `null` if undecided.
     */
    private fun isNotLessSpecificCallableReferenceDescriptor(f: CallableDescriptor, g: CallableDescriptor): Boolean {
        if (f.valueParameters.size != g.valueParameters.size) return false
        if (f.varargParameterPosition() != g.varargParameterPosition()) return false

        val fSignature = FlatSignature.createFromCallableDescriptor(f)
        val gSignature = FlatSignature.createFromCallableDescriptor(g)
        return createEmptyConstraintSystem().isSignatureNotLessSpecific(fSignature, gSignature, SpecificityComparisonWithNumerics, specificityComparator)
    }

    private fun isNotLessSpecificCallableReference(f: CallableDescriptor, g: CallableDescriptor): Boolean =
            // TODO should we "discriminate generic descriptors" for callable references?
            tryCompareDescriptorsFromScripts(f, g) ?:
            isNotLessSpecificCallableReferenceDescriptor(f, g)

    // Different smart casts may lead to the same candidate descriptor wrapped into different ResolvedCallImpl objects
    private fun uniquifyCandidatesSet(candidates: Collection<C>): Set<C> =
            THashSet(candidates.size, resolvedCallHashingStrategy).apply { addAll(candidates) }

    private fun newResolvedCallSet(expectedSize: Int): MutableSet<C> =
            THashSet(expectedSize, resolvedCallHashingStrategy)

    private fun FlatSignature<C>.candidateDescriptor() =
            origin.resultingDescriptor.original

    private fun FlatSignature<C>.descriptorVisibility() =
            candidateDescriptor().visibility
}
