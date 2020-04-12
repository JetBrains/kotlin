/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalTypeOrSubtype
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.calls.components.transformToResolvedLambda
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
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
) {
    enum class ConstraintSystemCompletionMode {
        FULL,
        PARTIAL
    }

    interface Context : VariableFixationFinder.Context, ResultTypeResolver.Context {
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>
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
        diagnosticsHolder: KotlinDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        runCompletion(
            c,
            completionMode,
            topLevelAtoms,
            topLevelType,
            diagnosticsHolder,
            collectVariablesFromContext = false,
            analyze = analyze
        )
    }

    fun completeConstraintSystem(
        c: Context,
        topLevelType: UnwrappedType,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        runCompletion(
            c,
            ConstraintSystemCompletionMode.FULL,
            topLevelAtoms,
            topLevelType,
            diagnosticsHolder,
            collectVariablesFromContext = true,
        ) {
            error("Shouldn't be called in complete constraint system mode")
        }
    }

    private fun runCompletion(
        c: Context,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        diagnosticsHolder: KotlinDiagnosticsHolder,
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
                c.resolveLambdaByAdditionalConditions(
                    variableForFixation,
                    topLevelAtoms,
                    diagnosticsHolder,
                    analyze,
                    variableFixationFinder
                )
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
                runCompletion(c, completionMode, topLevelAtoms, topLevelType, diagnosticsHolder, analyze)
            }
        }
    }

    /*
     * returns true -> analyzed
     */
    private fun Context.resolveLambdaByAdditionalConditions(
        variableForFixation: VariableFixationFinder.VariableForFixation,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit,
        fixationFinder: VariableFixationFinder
    ): Boolean {
        val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

        if (postponedArguments.isEmpty())
            return false

        val groupedPostponedArguments = postponedArguments.filterIsInstance<LambdaWithTypeVariableAsExpectedTypeAtom>()
            .ifEmpty { postponedArguments.filter { it !is LambdaWithTypeVariableAsExpectedTypeAtom } }

        if (groupedPostponedArguments.isEmpty())
            return false

        fun shouldAnalyzeLambdaWithTypeVariableAsExpectedType(argument: PostponedResolvedAtom): Boolean {
            val hasProperAtom = when (argument) {
                is LambdaWithTypeVariableAsExpectedTypeAtom, is PostponedCallableReferenceAtom ->
                    argument.expectedType?.constructor == variableForFixation.variable
                else -> false
            }

            return hasProperAtom || !variableForFixation.hasProperConstraint || variableForFixation.hasOnlyTrivialProperConstraint
        }

        fun shouldAnalyzeLambdaWhichIsReturnArgument(argument: PostponedResolvedAtom): Boolean {
            val isReturnArgumentOfAnotherLambda =
                argument is LambdaWithTypeVariableAsExpectedTypeAtom && argument.isReturnArgumentOfAnotherLambda
            val atomExpectedType = argument.expectedType

            return isReturnArgumentOfAnotherLambda && atomExpectedType != null &&
                    with(fixationFinder) { variableHasTrivialOrNonProperConstraints(atomExpectedType.constructor) }
        }

        return resolveLambdaByCondition(
            variableForFixation, groupedPostponedArguments, diagnosticsHolder, analyze, ::shouldAnalyzeLambdaWithTypeVariableAsExpectedType
        ) || resolveLambdaByCondition(
            variableForFixation, groupedPostponedArguments, diagnosticsHolder, analyze, ::shouldAnalyzeLambdaWhichIsReturnArgument
        )
    }

    /*
     * returns true -> analyzed
     */
    private fun Context.resolveLambdaByCondition(
        variableForFixation: VariableFixationFinder.VariableForFixation,
        postponedArguments: List<PostponedResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit,
        shouldAnalyze: (PostponedResolvedAtom) -> Boolean
    ): Boolean {
        val variable = variableForFixation.variable as? TypeConstructor ?: return false

        return postponedArguments.all { argument ->
            if (!shouldAnalyze(argument))
                return@all false

            val expectedTypeVariable = argument.expectedType?.constructor?.takeIf { it in allTypeVariables } ?: variable

            val preparedAtom =
                preparePostponedAtom(expectedTypeVariable, argument, expectedTypeVariable.builtIns, diagnosticsHolder)
                    ?: return@all false

            analyze(preparedAtom)

            true
        }
    }

    private fun Context.preparePostponedAtom(
        expectedTypeVariable: TypeConstructor,
        postponedAtom: PostponedResolvedAtom,
        builtIns: KotlinBuiltIns,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): PostponedResolvedAtom? {
        val csBuilder = (this as? NewConstraintSystem)?.getBuilder() ?: return null

        return when (postponedAtom) {
            is PostponedCallableReferenceAtom -> postponedAtom.preparePostponedAtomWithTypeVariableAsExpectedType(
                this, csBuilder, expectedTypeVariable,
                parameterTypes = null,
                isSuitable = KotlinType::isBuiltinFunctionalTypeOrSubtype,
                typeVariableCreator = { TypeVariableForCallableReferenceReturnType(builtIns, "_Q") },
                newAtomCreator = { returnVariable, expectedType ->
                    CallableReferenceWithTypeVariableAsExpectedTypeAtom(postponedAtom.atom, expectedType, returnVariable).also {
                        postponedAtom.setAnalyzedResults(null, listOf(it))
                    }
                }
            )
            is LambdaWithTypeVariableAsExpectedTypeAtom -> postponedAtom.preparePostponedAtomWithTypeVariableAsExpectedType(
                this, csBuilder, expectedTypeVariable,
                parameterTypes = postponedAtom.atom.parametersTypes,
                isSuitable = KotlinType::isBuiltinFunctionalType,
                typeVariableCreator = { TypeVariableForLambdaReturnType(postponedAtom.atom, builtIns, "_R") },
                newAtomCreator = { returnVariable, expectedType ->
                    postponedAtom.transformToResolvedLambda(csBuilder, diagnosticsHolder, expectedType, returnVariable)
                }
            )
            else -> null
        }
    }

    private inline fun <T : PostponedResolvedAtom, V : NewTypeVariable> T.preparePostponedAtomWithTypeVariableAsExpectedType(
        c: Context,
        csBuilder: ConstraintSystemBuilder,
        variable: TypeConstructor,
        parameterTypes: Array<out KotlinType?>?,
        isSuitable: KotlinType.() -> Boolean,
        typeVariableCreator: () -> V,
        newAtomCreator: (V, SimpleType) -> PostponedResolvedAtom
    ): PostponedResolvedAtom {
        val functionalType = resultTypeResolver.findResultType(
            c,
            c.notFixedTypeVariables.getValue(variable),
            TypeVariableDirectionCalculator.ResolveDirection.TO_SUPERTYPE
        ) as KotlinType

        val isExtensionFunction = functionalType.isExtensionFunctionType
        val isExtensionFunctionWithReceiverAsDeclaredParameter =
            isExtensionFunction && functionalType.arguments.size - 1 == parameterTypes?.count { it != null }
        if (parameterTypes?.all { it != null } == true && (!isExtensionFunction || isExtensionFunctionWithReceiverAsDeclaredParameter)) return this

        if (!functionalType.isSuitable()) return this
        val returnVariable = typeVariableCreator()
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
            ArgumentConstraintPosition(atom as KotlinCallArgument)
        )
        return newAtomCreator(returnVariable, expectedType)
    }

    // true if we do analyze
    private fun analyzePostponeArgumentIfPossible(
        c: Context,
        topLevelAtoms: List<ResolvedAtom>,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        for (argument in getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            if (canWeAnalyzeIt(c, argument)) {
                analyze(argument)
                return true
            }
        }
        return false
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
                is CallableReferenceWithTypeVariableAsExpectedTypeAtom -> mutableListOf<NewTypeVariable>().apply {
                    addIfNotNull(typeVariableForReturnType)
                    addAll(candidate?.freshSubstitutor?.freshVariables.orEmpty())
                }
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

    companion object {
        fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<ResolvedAtom>): List<PostponedResolvedAtom> {
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
    }
}