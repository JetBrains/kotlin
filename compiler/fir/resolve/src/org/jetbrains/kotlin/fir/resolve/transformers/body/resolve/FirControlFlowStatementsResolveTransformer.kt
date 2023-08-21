/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.isUnitOrFlexibleUnit
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.transformWhenSubjectExpressionUsingSmartcastInfo
import org.jetbrains.kotlin.fir.resolve.transformers.FirSyntheticCallGenerator
import org.jetbrains.kotlin.fir.resolve.transformers.FirWhenExhaustivenessTransformer
import org.jetbrains.kotlin.fir.resolve.withExpectedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.transformSingle

class FirControlFlowStatementsResolveTransformer(transformer: FirAbstractBodyResolveTransformerDispatcher) :
    FirPartialBodyResolveTransformer(transformer) {

    private val syntheticCallGenerator: FirSyntheticCallGenerator get() = components.syntheticCallGenerator
    private val whenExhaustivenessTransformer = FirWhenExhaustivenessTransformer(components)


    // ------------------------------- Loops -------------------------------

    override fun transformWhileLoop(whileLoop: FirWhileLoop, data: ResolutionMode): FirStatement {
        val context = ResolutionMode.ContextIndependent
        return whileLoop.also(dataFlowAnalyzer::enterWhileLoop)
            .transformCondition(transformer, withExpectedType(session.builtinTypes.booleanType))
            .also(dataFlowAnalyzer::exitWhileLoopCondition)
            .transformBlock(transformer, context).also(dataFlowAnalyzer::exitWhileLoop)
            .transformOtherChildren(transformer, context)
    }

    override fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: ResolutionMode): FirStatement {
        // Do-while has a specific scope structure (its block and condition effectively share the scope)
        return context.forBlock(session) {
            val context = ResolutionMode.ContextIndependent
            doWhileLoop.also(dataFlowAnalyzer::enterDoWhileLoop)
                .also {
                    transformer.expressionsTransformer?.transformBlockInCurrentScope(it.block, context)
                }
                .also(dataFlowAnalyzer::enterDoWhileLoopCondition)
                .transformCondition(transformer, withExpectedType(session.builtinTypes.booleanType))
                .also(dataFlowAnalyzer::exitDoWhileLoop)
                .transformOtherChildren(transformer, context)
        }
    }

    // ------------------------------- When expressions -------------------------------

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: ResolutionMode): FirStatement {
        if (whenExpression.calleeReference is FirResolvedNamedReference && whenExpression.resultType != null) {
            return whenExpression
        }
        whenExpression.annotations.forEach { it.accept(this, data) }
        dataFlowAnalyzer.enterWhenExpression(whenExpression)
        return context.withWhenExpression(whenExpression, session) with@{
            @Suppress("NAME_SHADOWING")
            var whenExpression = whenExpression.transformSubject(transformer, ResolutionMode.ContextIndependent)
            val subjectType = whenExpression.subject?.resolvedType?.fullyExpandedType(session)
            var completionNeeded = false
            context.withWhenSubjectType(subjectType, components) {
                when {
                    whenExpression.branches.isEmpty() -> {}
                    whenExpression.isOneBranch() && data.forceFullCompletion && data !is ResolutionMode.WithExpectedType -> {
                        whenExpression = whenExpression.transformBranches(transformer, ResolutionMode.ContextIndependent)
                        whenExpression.resultType = whenExpression.branches.first().result.resultType
                        // when with one branch cannot be completed if it's not already complete in the first place
                    }
                    else -> {
                        val resolutionModeForBranches =
                            (data as? ResolutionMode.WithExpectedType)
                                // Currently we don't use information from cast, but probably we could have
                                ?.takeUnless { it.fromCast }
                                ?.copy(forceFullCompletion = false)
                                ?: ResolutionMode.ContextDependent
                        whenExpression = whenExpression.transformBranches(
                            transformer,
                            resolutionModeForBranches,
                        )

                        whenExpression = syntheticCallGenerator.generateCalleeForWhenExpression(whenExpression, resolutionContext)
                        completionNeeded = true
                    }
                }
                whenExpression = whenExpression.transformSingle(whenExhaustivenessTransformer, null)

                // This is necessary to perform outside the place where the synthetic call is created because
                // exhaustiveness is not yet computed there, but at the same time to compute it properly
                // we need having branches condition bes analyzed that is why we can't have call
                // `whenExpression.transformSingle(whenExhaustivenessTransformer, null)` at the beginning
                if (completionNeeded) {
                    val completionResult = callCompleter.completeCall(
                        whenExpression,
                        // For non-exhaustive when expressions, we should complete then as independent because below
                        // their type is artificially replaced with Unit, while candidate symbol's return type remains the same
                        // So when combining two when's the inner one was erroneously resolved as a normal dependent exhaustive sub-expression
                        // At the same time, it all looks suspicious and inconsistent, so we hope it would be investigated at KT-55175
                        if (whenExpression.isProperlyExhaustive) data else ResolutionMode.ContextIndependent,
                    )
                    whenExpression = completionResult
                }
                dataFlowAnalyzer.exitWhenExpression(whenExpression, data.forceFullCompletion)
                whenExpression = whenExpression.replaceReturnTypeIfNotExhaustive()
                whenExpression
            }
        }
    }

    private fun FirWhenExpression.replaceReturnTypeIfNotExhaustive(): FirWhenExpression {
        if (!isProperlyExhaustive) {
            resultType = session.builtinTypes.unitType.type
        }
        return this
    }

    private fun FirWhenExpression.isOneBranch(): Boolean {
        if (branches.size == 1) return true
        if (branches.size > 2) return false
        val lastBranch = branches.last()
        return lastBranch.source != null && lastBranch.condition is FirElseIfTrueCondition && lastBranch.result is FirEmptyExpressionBlock
    }

    override fun transformWhenBranch(whenBranch: FirWhenBranch, data: ResolutionMode): FirWhenBranch {
        dataFlowAnalyzer.enterWhenBranchCondition(whenBranch)
        return context.withWhenSubjectImportingScope {
            whenBranch.transformCondition(transformer, withExpectedType(session.builtinTypes.booleanType))
        }.also { dataFlowAnalyzer.exitWhenBranchCondition(it) }
            .transformResult(transformer, data)
            .also { dataFlowAnalyzer.exitWhenBranchResult(it) }

    }

    override fun transformWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: ResolutionMode
    ): FirStatement {
        val parentWhen = whenSubjectExpression.whenRef.value
        val subjectType = parentWhen.subject?.resultType ?: parentWhen.subjectVariable?.returnTypeRef?.coneTypeOrNull
        if (subjectType != null) {
            whenSubjectExpression.resultType = subjectType
        }
        dataFlowAnalyzer.exitWhenSubjectExpression(whenSubjectExpression)
        return components.transformWhenSubjectExpressionUsingSmartcastInfo(whenSubjectExpression)
    }

    // ------------------------------- Try/catch expressions -------------------------------

    override fun transformTryExpression(tryExpression: FirTryExpression, data: ResolutionMode): FirStatement {
        if (tryExpression.calleeReference is FirResolvedNamedReference && tryExpression.resultType != null) {
            return tryExpression
        }

        tryExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        dataFlowAnalyzer.enterTryExpression(tryExpression)
        tryExpression.transformTryBlock(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitTryMainBlock()
        tryExpression.transformCatches(this, ResolutionMode.ContextDependent)

        val incomplete = syntheticCallGenerator.generateCalleeForTryExpression(tryExpression, resolutionContext)
        var result = callCompleter.completeCall(incomplete, data)
        if (result.finallyBlock != null) {
            dataFlowAnalyzer.enterFinallyBlock()
            result = result.transformFinallyBlock(transformer, ResolutionMode.ContextIndependent)
            dataFlowAnalyzer.exitFinallyBlock()
        }
        dataFlowAnalyzer.exitTryExpression(data.forceFullCompletion)
        return result
    }

    override fun transformCatch(catch: FirCatch, data: ResolutionMode): FirCatch {
        dataFlowAnalyzer.enterCatchClause(catch)
        catch.parameter.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
        return context.forBlock(session) {
            catch.transformParameter(transformer, ResolutionMode.ContextIndependent)
            catch.transformBlock(transformer, ResolutionMode.ContextDependent)
        }.also { dataFlowAnalyzer.exitCatchClause(it) }
    }

    // ------------------------------- Jumps -------------------------------

    override fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: ResolutionMode): FirStatement {
        dataFlowAnalyzer.enterJump(jump)
        val result = transformer.transformExpression(jump, data)
        dataFlowAnalyzer.exitJump(jump)
        return result
    }

    override fun transformReturnExpression(
        returnExpression: FirReturnExpression,
        data: ResolutionMode
    ): FirStatement {
        val labeledElement = returnExpression.target.labeledElement
        val expectedTypeRef = labeledElement.returnTypeRef

        val mode = when {
            labeledElement.symbol in context.anonymousFunctionsAnalyzedInDependentContext -> ResolutionMode.ContextDependent

            expectedTypeRef is FirResolvedTypeRef ->
                ResolutionMode.WithExpectedType(expectedTypeRef, expectedTypeMismatchIsReportedInChecker = true)

            else -> ResolutionMode.ContextIndependent
        }

        return transformJump(returnExpression, mode)
    }

    override fun transformThrowExpression(
        throwExpression: FirThrowExpression,
        data: ResolutionMode,
    ): FirStatement {
        return throwExpression.apply {
            transformAnnotations(transformer, data)
            transformException(transformer, withExpectedType(session.builtinTypes.throwableType))
            dataFlowAnalyzer.exitThrowExceptionNode(this)
        }
    }

    // ------------------------------- Elvis -------------------------------

    override fun transformElvisExpression(
        elvisExpression: FirElvisExpression,
        data: ResolutionMode
    ): FirStatement {
        if (elvisExpression.calleeReference is FirResolvedNamedReference) return elvisExpression
        elvisExpression.transformAnnotations(transformer, data)

        // Do not use expect type when it's came from if/when (when it doesn't require completion)
        // It returns us to K1 behavior in the case of when-elvis combination (see testData/diagnostics/tests/inference/elvisInsideWhen.kt)
        // But this is mostly a hack that I hope might be lifted once KT-55692 is considered
        // NB: Currently situation `it is ResolutionMode.WithExpectedType && !it.forceFullCompletion` might only happen in case of when branches
        @Suppress("NAME_SHADOWING")
        val data = data.takeUnless { it is ResolutionMode.WithExpectedType && !it.forceFullCompletion } ?: ResolutionMode.ContextDependent

        val expectedType = data.expectedType?.coneTypeSafe<ConeKotlinType>()
        val mayBeCoercionToUnitApplied = (data as? ResolutionMode.WithExpectedType)?.mayBeCoercionToUnitApplied == true

        val resolutionModeForLhs =
            if (mayBeCoercionToUnitApplied && expectedType?.isUnitOrFlexibleUnit == true)
                withExpectedType(expectedType, mayBeCoercionToUnitApplied = true)
            else
                withExpectedType(expectedType?.withNullability(ConeNullability.NULLABLE, session.typeContext))
        dataFlowAnalyzer.enterElvis(elvisExpression)
        elvisExpression.transformLhs(transformer, resolutionModeForLhs)
        dataFlowAnalyzer.exitElvisLhs(elvisExpression)

        val resolutionModeForRhs = withExpectedType(
            expectedType,
            mayBeCoercionToUnitApplied = mayBeCoercionToUnitApplied
        )
        elvisExpression.transformRhs(transformer, resolutionModeForRhs)

        val result = callCompleter.completeCall(
            syntheticCallGenerator.generateCalleeForElvisExpression(elvisExpression, resolutionContext), data
        )

        var isLhsNotNull = false
        if (result.rhs.coneTypeSafe<ConeKotlinType>()?.isNothing == true) {
            val lhsType = result.lhs.coneTypeSafe<ConeKotlinType>()
            if (lhsType != null) {
                // Converting to non-raw type is necessary to preserver the K1 semantics (see KT-54526)
                val newReturnType =
                    lhsType.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext)
                        .convertToNonRawVersion()
                result.replaceConeTypeOrNull(newReturnType)
                isLhsNotNull = true
            }
        }

        session.typeContext.run {
            if (result.coneTypeSafe<ConeKotlinType>()?.isNullableType() == true
                && result.rhs.coneTypeSafe<ConeKotlinType>()?.isNullableType() == false
            ) {
                // Sometimes return type for special call for elvis operator might be nullable,
                // but result is not nullable if the right type is not nullable
                result.replaceConeTypeOrNull(
                    result.resolvedType.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext)
                )
            }
        }

        dataFlowAnalyzer.exitElvis(elvisExpression, isLhsNotNull, data.forceFullCompletion)
        return result
    }
}
