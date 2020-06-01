/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val Candidate.csBuilder: NewConstraintSystemImpl get() = system.getBuilder()


class ConstraintSystemCompleter(private val components: BodyResolveComponents) {
    private val variableFixationFinder = VariableFixationFinder(components.inferenceComponents.trivialConstraintTypeInferenceOracle)

    fun complete(
        c: KotlinConstraintSystemCompleter.Context,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<FirStatement>,
        candidateReturnType: ConeKotlinType,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {

        while (true) {
            if (analyzePostponeArgumentIfPossible(c, topLevelAtoms, analyze)) continue

            val allTypeVariables = getOrderedAllTypeVariables(c, topLevelAtoms)
            val postponedAtoms = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)
            val variableForFixation =
                variableFixationFinder.findFirstVariableForFixation(
                    c, allTypeVariables, postponedAtoms, completionMode, candidateReturnType
                ) ?: break

            if (
                completionMode == ConstraintSystemCompletionMode.FULL &&
                resolveLambdaOrCallableReferenceWithTypeVariableAsExpectedType(c, variableForFixation, postponedAtoms, analyze)
            ) {
                continue
            }

            if (variableForFixation.hasProperConstraint || completionMode == ConstraintSystemCompletionMode.FULL) {
                val variableWithConstraints = c.notFixedTypeVariables.getValue(variableForFixation.variable)

                fixVariable(c, candidateReturnType, variableWithConstraints, emptyList())

//                if (!variableForFixation.hasProperConstraint) {
//                    c.addError(NotEnoughInformationForTypeParameter(variableWithConstraints.typeVariable))
//                }
                continue
            }

            break
        }

        if (completionMode == ConstraintSystemCompletionMode.FULL) {
            // force resolution for all not-analyzed argument's
            getOrderedNotAnalyzedPostponedArguments(topLevelAtoms).forEach(analyze)
//
//            if (c.notFixedTypeVariables.isNotEmpty() && c.postponedTypeVariables.isEmpty()) {
//                runCompletion(c, completionMode, topLevelAtoms, topLevelType, analyze)
//            }
        }
    }

    private fun resolveLambdaOrCallableReferenceWithTypeVariableAsExpectedType(
        c: KotlinConstraintSystemCompleter.Context,
        variableForFixation: VariableFixationFinder.VariableForFixation,
        postponedAtoms: List<PostponedResolvedAtom>,
        /*diagnosticsHolder: KotlinDiagnosticsHolder,*/
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        val variable = variableForFixation.variable as ConeTypeVariableTypeConstructor
        val hasProperAtom = postponedAtoms.any {
            when (it) {
                is LambdaWithTypeVariableAsExpectedTypeAtom/*, is PostponedCallableReferenceAtom*/
                -> it.expectedType.typeConstructor(c) == variable // TODO
                else -> false
            }
        }
        if (
            !hasProperAtom &&
            variableForFixation.hasProperConstraint &&
            !variableForFixation.hasOnlyTrivialProperConstraint
        ) return false

        val postponedAtom = postponedAtoms.firstOrNull() ?: return false
        val csBuilder = (c as NewConstraintSystemImpl).getBuilder()
        val expectedTypeVariableConstructor = postponedAtom.expectedType?.typeConstructor(c)?.takeIf { it in c.allTypeVariables } as? ConeTypeVariableTypeConstructor ?: variable
        val expectedTypeVariable = csBuilder.currentStorage().allTypeVariables[expectedTypeVariableConstructor] as ConeTypeVariable? ?: return false

        val atomToAnalyze = when (postponedAtom) {
            is LambdaWithTypeVariableAsExpectedTypeAtom -> {
                postponedAtom.preparePostponedAtomWithTypeVariableAsExpectedType(
                    c, csBuilder, expectedTypeVariable,
                    parameterTypes = null,
                    isSuitable = { isBuiltinFunctionalType(components.session) },
                    typeVariableCreator = { ConeTypeVariableForLambdaReturnType(postponedAtom.atom, "_R") },
                    newAtomCreator = { returnTypeVariable, expectedType ->
                        postponedAtom.transformToResolvedLambda(csBuilder, expectedType, returnTypeVariable)
                    }
                )
            }
            // is PostponedCallableReferenceAtom -> TODO()
            else -> return false
        }
        analyze(atomToAnalyze)
        return true
    }

    private inline fun <T : PostponedResolvedAtom, V : ConeTypeVariable> T.preparePostponedAtomWithTypeVariableAsExpectedType(
        c: KotlinConstraintSystemCompleter.Context,
        csBuilder: ConstraintSystemBuilder,
        variable: ConeTypeVariable,
        parameterTypes: Array<out ConeKotlinType?>?,
        isSuitable: ConeKotlinType.() -> Boolean,
        typeVariableCreator: () -> V,
        newAtomCreator: (V, ConeKotlinType) -> PostponedResolvedAtom
    ): PostponedResolvedAtom {
        val functionalType = (components.inferenceComponents.resultTypeResolver.findResultType(
            c,
            c.notFixedTypeVariables.getValue(variable.typeConstructor),
            TypeVariableDirectionCalculator.ResolveDirection.TO_SUPERTYPE
        ) as ConeKotlinType).lowerBoundIfFlexible() as ConeClassLikeType
        val isExtensionWithoutParameters = false
//        TODO
//             functionalType.isExtensionFunctionType && functionalType.arguments.size == 2 && parameterTypes?.isEmpty() == true
        if (parameterTypes?.all { type -> type != null } == true && !isExtensionWithoutParameters) return this
        if (!functionalType.isSuitable()) return this
        val returnVariable = typeVariableCreator()
        csBuilder.registerVariable(returnVariable)

        val expectedType = ConeClassLikeTypeImpl(
            lookupTag = functionalType.lookupTag,
            typeArguments = (functionalType.typeArguments.dropLast(1) + returnVariable.defaultType).toTypedArray(),
            isNullable = functionalType.isNullable
        )

        csBuilder.addSubtypeConstraint(
            expectedType,
            variable.defaultType,
            SimpleConstraintSystemConstraintPosition
//            ArgumentConstraintPosition(atom as KotlinCallArgument)
        )
        return newAtomCreator(returnVariable, expectedType)
    }


    private fun getOrderedAllTypeVariables(
        c: KotlinConstraintSystemCompleter.Context,
        topLevelAtoms: List<FirStatement>
    ): List<TypeConstructorMarker> {
        val result = LinkedHashSet<TypeConstructorMarker>(c.notFixedTypeVariables.size)
        fun ConeTypeVariable?.toTypeConstructor(): TypeConstructorMarker? =
            this?.typeConstructor?.takeIf { it in c.notFixedTypeVariables.keys }

        fun FirStatement.collectAllTypeVariables() {
            this.processAllContainingCallCandidates(processBlocks = true) { candidate ->
                candidate.freshVariables.mapNotNullTo(result) { typeVariable ->
                    typeVariable.toTypeConstructor()
                }

                for (postponedAtom in candidate.postponedAtoms) {
                    when (postponedAtom) {
                        is ResolvedLambdaAtom -> postponedAtom.typeVariableForLambdaReturnType
                    }
                }
                for (lambdaAtom in candidate.postponedAtoms.filterIsInstance<ResolvedLambdaAtom>()) {
                    result.addIfNotNull(lambdaAtom.typeVariableForLambdaReturnType.toTypeConstructor())
                }
            }
        }

        for (topLevel in topLevelAtoms) {
            topLevel.collectAllTypeVariables()
        }

        require(result.size == c.notFixedTypeVariables.size) {
            val notFoundTypeVariables = c.notFixedTypeVariables.keys.toMutableSet().apply { removeAll(result) }
            "Not all type variables found: $notFoundTypeVariables"
        }

        return result.toList()
    }

    private fun fixVariable(
        c: KotlinConstraintSystemCompleter.Context,
        topLevelType: KotlinTypeMarker,
        variableWithConstraints: VariableWithConstraints,
        postponedResolveKtPrimitives: List<PostponedResolvedAtom>
    ) {
        val direction = TypeVariableDirectionCalculator(c, postponedResolveKtPrimitives, topLevelType).getDirection(variableWithConstraints)
        fixVariable(c, variableWithConstraints, direction)
    }

    fun fixVariable(
        c: KotlinConstraintSystemCompleter.Context,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection
    ) {
        val resultType = components.inferenceComponents.resultTypeResolver.findResultType(c, variableWithConstraints, direction)
        c.fixVariable(variableWithConstraints.typeVariable, resultType, atom = null) // TODO: obtain atom for diagnostics
    }

    private fun analyzePostponeArgumentIfPossible(
        c: KotlinConstraintSystemCompleter.Context,
        topLevelAtoms: List<FirStatement>,
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

    private fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<FirStatement>): List<PostponedResolvedAtom> {
        val notAnalyzedArguments = arrayListOf<PostponedResolvedAtom>()
        for (primitive in topLevelAtoms) {
            primitive.processAllContainingCallCandidates(
                // TODO: remove this argument and relevant parameter
                // Currently, it's used because otherwise problem happens with a lambda in a try-block (see tryWithLambdaInside test)
                processBlocks = true
            ) { candidate ->
                candidate.postponedAtoms.forEach {
                    notAnalyzedArguments.addIfNotNull(it.safeAs<PostponedResolvedAtom>()?.takeUnless { it.analyzed })
                }
            }
        }

        return notAnalyzedArguments
    }

    private fun FirStatement.processAllContainingCallCandidates(processBlocks: Boolean, processor: (Candidate) -> Unit) {
        when (this) {
            is FirFunctionCall -> {
                processCandidateIfApplicable(processor, processBlocks)
                this.arguments.forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
            }

            is FirSafeCallExpression -> {
                this.regularQualifiedAccess.processAllContainingCallCandidates(processBlocks, processor)
            }

            is FirWhenExpression -> {
                processCandidateIfApplicable(processor, processBlocks)
                this.branches.forEach { it.result.processAllContainingCallCandidates(processBlocks, processor) }
            }

            is FirTryExpression -> {
                processCandidateIfApplicable(processor, processBlocks)
                tryBlock.processAllContainingCallCandidates(processBlocks, processor)
                catches.forEach { it.block.processAllContainingCallCandidates(processBlocks, processor) }
            }

            is FirCheckNotNullCall -> {
                processCandidateIfApplicable(processor, processBlocks)
                this.arguments.forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
            }

            is FirQualifiedAccessExpression -> {
                processCandidateIfApplicable(processor, processBlocks)
            }

            is FirVariableAssignment -> {
                processCandidateIfApplicable(processor, processBlocks)
                rValue.processAllContainingCallCandidates(processBlocks, processor)
            }

            is FirWrappedArgumentExpression -> this.expression.processAllContainingCallCandidates(processBlocks, processor)
            is FirBlock -> {
                if (processBlocks) {
                    this.returnExpressions().forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
                }
            }

            is FirDelegatedConstructorCall -> {
                processCandidateIfApplicable(processor, processBlocks)
                this.arguments.forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
            }
        }
    }

    private fun FirResolvable.processCandidateIfApplicable(
        processor: (Candidate) -> Unit,
        processBlocks: Boolean
    ) {
        val candidate = (calleeReference as? FirNamedReferenceWithCandidate)?.candidate ?: return
        processor(candidate)

        for (atom in candidate.postponedAtoms) {
            if (atom !is ResolvedLambdaAtom || !atom.analyzed) continue

            atom.returnStatements.forEach {
                it.processAllContainingCallCandidates(processBlocks, processor)
            }
        }
    }

    private fun canWeAnalyzeIt(c: KotlinConstraintSystemCompleter.Context, argument: PostponedResolvedAtomMarker): Boolean {
        if (argument.analyzed) return false

        return argument.inputTypes.all { c.containsOnlyFixedOrPostponedVariables(it) }
    }

}
