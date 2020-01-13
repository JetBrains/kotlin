/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionStatelessCallbacks
import org.jetbrains.kotlin.resolve.calls.components.transformToResolvedLambda
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KotlinConstraintSystemCompleter(
    private val resultTypeResolver: ResultTypeResolver,
    val variableFixationFinder: VariableFixationFinder,
    private val statelessCallbacks: KotlinResolutionStatelessCallbacks
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

    fun completeConstraintSystem(c: Context, topLevelType: UnwrappedType, topLevelAtoms: List<ResolvedAtom>) {
        runCompletion(c, ConstraintSystemCompletionMode.FULL, topLevelAtoms, topLevelType, collectVariablesFromContext = true) {
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

            if (
                completionMode == ConstraintSystemCompletionMode.FULL &&
                resolveLambdaOrCallableReferenceWithTypeVariableAsExpectedType(c, variableForFixation, topLevelAtoms, analyze)
            ) {
                continue
            }

            if (variableForFixation.hasProperConstraint || completionMode == ConstraintSystemCompletionMode.FULL) {
                val variableWithConstraints = c.notFixedTypeVariables.getValue(variableForFixation.variable)

                if (variableForFixation.hasProperConstraint)
                    fixVariable(c, variableWithConstraints, topLevelAtoms)
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

    /*
     * returns true -> analyzed
     */
    private fun resolveLambdaOrCallableReferenceWithTypeVariableAsExpectedType(
        c: Context,
        variableForFixation: VariableFixationFinder.VariableForFixation,
        topLevelAtoms: List<ResolvedAtom>,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        val variable = variableForFixation.variable as TypeConstructor
        val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)
        if (
            !postponedArguments.any { (it as? LambdaWithTypeVariableAsExpectedTypeAtom)?.expectedType?.constructor == variable } &&
            variableForFixation.hasProperConstraint &&
            !variableForFixation.hasOnlyTrivialProperConstraint
        ) return false

        val postponedAtom = postponedArguments.firstOrNull() ?: return false
        when (postponedAtom) {
            is PostponedCallableReferenceAtom -> {
                analyze(postponedAtom)
            }
            is LambdaWithTypeVariableAsExpectedTypeAtom -> {
                var atomToAnalyze = postponedAtom
                if (postponedAtom.atom.parametersTypes?.all { it != null } != true) {
                    val functionalType = resultTypeResolver.findResultType(
                        c,
                        c.notFixedTypeVariables.getValue(variable),
                        TypeVariableDirectionCalculator.ResolveDirection.TO_SUPERTYPE
                    ) as KotlinType
                    if (functionalType.isBuiltinFunctionalType) {
                        val csBuilder = c as ConstraintSystemBuilder
                        val builtIns = (variable as TypeVariableTypeConstructor).builtIns
                        val returnVariable = TypeVariableForLambdaReturnType(postponedAtom.atom, builtIns, "_R")
                        csBuilder.registerVariable(returnVariable)
                        val expectedType = KotlinTypeFactory.simpleType(
                            functionalType.annotations,
                            functionalType.constructor,
                            functionalType.arguments.dropLast(1) + returnVariable.defaultType.asTypeProjection(),
                            functionalType.isMarkedNullable
                        )
                        csBuilder.addSubtypeConstraint(
                            expectedType,
                            variable.typeForTypeVariable(),
                            ArgumentConstraintPosition(postponedAtom.atom)
                        )
                        atomToAnalyze = postponedAtom.transformToResolvedLambda(csBuilder, expectedType, returnVariable)
                    }
                }
                analyze(atomToAnalyze)
            }
            else -> return false
        }
        return true
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

            /*
             * Hack for completing error candidates in delegate resolve
             */
            if (this is StubResolvedAtom && typeVariable in c.notFixedTypeVariables) {
                to += typeVariable
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
            val notFoundTypeVariables = c.notFixedTypeVariables.keys.toMutableSet().apply { removeAll(result) }
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
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>
    ) {
        fixVariable(c, variableWithConstraints, TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN, topLevelAtoms)
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