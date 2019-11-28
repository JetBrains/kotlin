/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KotlinConstraintSystemCompleter(
    private val resultTypeResolver: ResultTypeResolver,
    private val variableFixationFinder: VariableFixationFinder
) {
    enum class ConstraintSystemCompletionMode {
        FULL,
        PARTIAL
    }

    interface Context : VariableFixationFinder.Context, ResultTypeResolver.Context {
        override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

        override val postponedTypeVariables: List<TypeVariableMarker>

        // type can be proper if it not contains not fixed type variables
        fun canBeProper(type: KotlinTypeMarker): Boolean

        fun containsOnlyFixedOrPostponedVariables(type: KotlinTypeMarker): Boolean

        // mutable operations
        fun addError(error: KotlinCallDiagnostic)

        fun fixVariable(variable: TypeVariableMarker, resultType: KotlinTypeMarker, atom: ResolvedAtom?)
    }

    fun runCompletion(
        c: Context,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        runCompletion(c, completionMode, topLevelAtoms, topLevelType, collectVariablesFromContext = false, analyze = analyze)
    }

    fun completeConstraintSystem(c: Context, topLevelType: UnwrappedType) {
        runCompletion(c, ConstraintSystemCompletionMode.FULL, emptyList(), topLevelType, collectVariablesFromContext = true) {
            error("Shouldn't be called in complete constraint system mode")
        }
    }

    private fun runCompletion(
        c: Context,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        while (true) {
            if (analyzePostponeArgumentIfPossible(c, topLevelAtoms, analyze)) continue

            val allTypeVariables = getOrderedAllTypeVariables(c, collectVariablesFromContext, topLevelAtoms)
            val postponedKtPrimitives = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)
            val variableForFixation =
                variableFixationFinder.findFirstVariableForFixation(
                    c, allTypeVariables, postponedKtPrimitives, completionMode, topLevelType
                ) ?: break

            if (shouldForceCallableReferenceOrLambdaResolution(completionMode, variableForFixation)) {
                if (forcePostponedAtomResolution<PostponedCallableReferenceAtom>(topLevelAtoms, analyze)) continue
                if (forcePostponedAtomResolution<LambdaWithTypeVariableAsExpectedTypeAtom>(topLevelAtoms, analyze)) continue
            }

            if (variableForFixation.hasProperConstraint || completionMode == ConstraintSystemCompletionMode.FULL) {
                val variableWithConstraints = c.notFixedTypeVariables.getValue(variableForFixation.variable)

                if (variableForFixation.hasProperConstraint)
                    fixVariable(c, topLevelType, variableWithConstraints, postponedKtPrimitives, topLevelAtoms)
                else
                    processVariableWhenNotEnoughInformation(c, variableWithConstraints, topLevelAtoms)

                continue
            }

            break
        }

        if (completionMode == ConstraintSystemCompletionMode.FULL) {
            // force resolution for all not-analyzed argument's
            getOrderedNotAnalyzedPostponedArguments(topLevelAtoms).forEach(analyze)

            if (c.notFixedTypeVariables.isNotEmpty() && c.postponedTypeVariables.isEmpty()) {
                runCompletion(c, completionMode, topLevelAtoms, topLevelType, analyze)
            }
        }
    }

    private fun shouldForceCallableReferenceOrLambdaResolution(
        completionMode: ConstraintSystemCompletionMode,
        variableForFixation: VariableFixationFinder.VariableForFixation
    ): Boolean {
        if (completionMode == ConstraintSystemCompletionMode.PARTIAL) return false
        return !variableForFixation.hasProperConstraint || variableForFixation.hasOnlyTrivialProperConstraint
    }

    // true if we do analyze
    private fun analyzePostponeArgumentIfPossible(
        c: Context,
        topLevelAtoms: List<ResolvedAtom>,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        for (argument in getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)) {
            if (canWeAnalyzeIt(c, argument)) {
                analyze(argument)
                return true
            }
        }
        return false
    }

    // true if we find some callable reference and run resolution for it. Note that such resolution can be unsuccessful
    private inline fun <reified T : PostponedResolvedAtom> forcePostponedAtomResolution(
        topLevelAtoms: List<ResolvedAtom>,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        val postponedArgument = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms).firstIsInstanceOrNull<T>() ?: return false
        analyze(postponedArgument)
        return true
    }

    private fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<ResolvedAtom>): List<PostponedResolvedAtom> {
        fun ResolvedAtom.process(to: MutableList<PostponedResolvedAtom>) {
            to.addIfNotNull(this.safeAs<PostponedResolvedAtom>()?.takeUnless { it.analyzed })

            if (analyzed) {
                subResolvedAtoms?.forEach { it.process(to) }
            }
        }

        val notAnalyzedArguments = arrayListOf<PostponedResolvedAtom>()
        for (primitive in topLevelAtoms) {
            primitive.process(notAnalyzedArguments)
        }

        return notAnalyzedArguments
    }

    private fun getOrderedAllTypeVariables(
        c: Context,
        collectVariablesFromContext: Boolean,
        topLevelAtoms: List<ResolvedAtom>
    ): List<TypeConstructorMarker> {
        if (collectVariablesFromContext) return c.notFixedTypeVariables.keys.toList()

        fun ResolvedAtom.process(to: LinkedHashSet<TypeConstructor>) {
            val typeVariables = when (this) {
                is ResolvedCallAtom -> freshVariablesSubstitutor.freshVariables
                is ResolvedCallableReferenceAtom -> candidate?.freshSubstitutor?.freshVariables.orEmpty()
                is ResolvedLambdaAtom -> listOfNotNull(typeVariableForLambdaReturnType)
                else -> emptyList()
            }
            typeVariables.mapNotNullTo(to) {
                val typeConstructor = it.freshTypeConstructor
                typeConstructor.takeIf { c.notFixedTypeVariables.containsKey(typeConstructor) }
            }

            if (analyzed) {
                subResolvedAtoms?.forEach { it.process(to) }
            }
        }

        // Note that it's important to use Set here, because several atoms can share the same type variable
        val result = linkedSetOf<TypeConstructor>()
        for (primitive in topLevelAtoms) {
            primitive.process(result)
        }

        assert(result.size == c.notFixedTypeVariables.size) {
            val notFoundTypeVariables = c.notFixedTypeVariables.keys.toMutableSet().removeAll(result)
            "Not all type variables found: $notFoundTypeVariables"
        }

        return result.toList()
    }


    private fun canWeAnalyzeIt(c: Context, argument: PostponedResolvedAtom): Boolean {
        if (argument.analyzed) return false

        return argument.inputTypes.all { c.containsOnlyFixedOrPostponedVariables(it) }
    }

    private fun fixVariable(
        c: Context,
        topLevelType: UnwrappedType,
        variableWithConstraints: VariableWithConstraints,
        postponedResolveKtPrimitives: List<PostponedResolvedAtom>,
        topLevelAtoms: List<ResolvedAtom>
    ) {
        val direction = TypeVariableDirectionCalculator(c, postponedResolveKtPrimitives, topLevelType).getDirection(variableWithConstraints)
        fixVariable(c, variableWithConstraints, direction, topLevelAtoms)
    }

    fun fixVariable(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection,
        topLevelAtoms: List<ResolvedAtom>
    ) {
        val resultType = resultTypeResolver.findResultType(c, variableWithConstraints, direction)
        val resolvedAtom = findResolvedAtomBy(variableWithConstraints.typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()
        c.fixVariable(variableWithConstraints.typeVariable, resultType, resolvedAtom)
    }

    private fun processVariableWhenNotEnoughInformation(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>
    ) {
        val typeVariable = variableWithConstraints.typeVariable

        val resolvedAtom = findResolvedAtomBy(typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()
        if (resolvedAtom != null) {
            c.addError(NotEnoughInformationForTypeParameter(typeVariable, resolvedAtom))
        }

        val resultErrorType = if (typeVariable is TypeVariableFromCallableDescriptor)
            ErrorUtils.createUninferredParameterType(typeVariable.originalTypeParameter)
        else
            ErrorUtils.createErrorType("Cannot infer type variable $typeVariable")

        c.fixVariable(typeVariable, resultErrorType, resolvedAtom)
    }

    private fun findResolvedAtomBy(typeVariable: TypeVariableMarker, topLevelAtoms: List<ResolvedAtom>): ResolvedAtom? {
        fun ResolvedAtom.check(): ResolvedAtom? {
            val suitableCall = when (this) {
                is ResolvedCallAtom -> typeVariable in freshVariablesSubstitutor.freshVariables
                is ResolvedCallableReferenceAtom -> candidate?.freshSubstitutor?.freshVariables?.let { typeVariable in it } ?: false
                is ResolvedLambdaAtom -> typeVariable == typeVariableForLambdaReturnType
                else -> false
            }

            if (suitableCall) {
                return this
            }

            subResolvedAtoms?.forEach { subResolvedAtom ->
                subResolvedAtom.check()?.let { result -> return@check result }
            }

            return null
        }

        for (topLevelAtom in topLevelAtoms) {
            topLevelAtom.check()?.let { return it }
        }

        return null
    }
}