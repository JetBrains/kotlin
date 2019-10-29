/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.transformers.FirSyntheticCallGenerator
import org.jetbrains.kotlin.fir.resolve.transformers.FirWhenExhaustivenessTransformer
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle

class FirControlFlowStatementsResolveTransformer(transformer: FirBodyResolveTransformer) :
    FirPartialBodyResolveTransformer(transformer) {

    private val syntheticCallGenerator: FirSyntheticCallGenerator get() = components.syntheticCallGenerator
    private val whenExhaustivenessTransformer = FirWhenExhaustivenessTransformer(components)


    // ------------------------------- Loops -------------------------------

    override fun transformWhileLoop(whileLoop: FirWhileLoop, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return whileLoop.also(dataFlowAnalyzer::enterWhileLoop)
            .transformCondition(transformer, data).also(dataFlowAnalyzer::exitWhileLoopCondition)
            .transformBlock(transformer, data).also(dataFlowAnalyzer::exitWhileLoop)
            .transformOtherChildren(transformer, data).compose()
    }

    override fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return doWhileLoop.also(dataFlowAnalyzer::enterDoWhileLoop)
            .transformBlock(transformer, data).also(dataFlowAnalyzer::enterDoWhileLoopCondition)
            .transformCondition(transformer, data).also(dataFlowAnalyzer::exitDoWhileLoop)
            .transformOtherChildren(transformer, data).compose()
    }

    // ------------------------------- When expressions -------------------------------

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (whenExpression.calleeReference is FirResolvedNamedReference && whenExpression.resultType !is FirImplicitTypeRef) {
            return whenExpression.compose()
        }
        dataFlowAnalyzer.enterWhenExpression(whenExpression)
        return withScopeCleanup(localScopes) with@{
            if (whenExpression.subjectVariable != null) {
                localScopes += FirLocalScope()
            }
            @Suppress("NAME_SHADOWING")
            var whenExpression = whenExpression.transformSubject(transformer, ResolutionMode.ContextIndependent)

            when {
                whenExpression.branches.isEmpty() -> {
                }
                whenExpression.isOneBranch() -> {
                    whenExpression = whenExpression.transformBranches(transformer, ResolutionMode.ContextIndependent)
                    whenExpression.resultType = whenExpression.branches.first().result.resultType
                }
                else -> {
                    whenExpression = whenExpression.transformBranches(transformer, ResolutionMode.ContextDependent)

                    whenExpression = syntheticCallGenerator.generateCalleeForWhenExpression(whenExpression) ?: run {
                        whenExpression = whenExpression.transformSingle(whenExhaustivenessTransformer, null)
                        dataFlowAnalyzer.exitWhenExpression(whenExpression)
                        whenExpression.resultType = FirErrorTypeRefImpl(null, FirSimpleDiagnostic("Can't resolve when expression", DiagnosticKind.InferenceError))
                        return@with whenExpression.compose()
                    }

                    val expectedTypeRef = data.expectedType
                    whenExpression = callCompleter.completeCall(whenExpression, expectedTypeRef)
                }
            }
            whenExpression = whenExpression.transformSingle(whenExhaustivenessTransformer, null)
            dataFlowAnalyzer.exitWhenExpression(whenExpression)
            whenExpression = whenExpression.replaceReturnTypeIfNotExhaustive()
            whenExpression.compose()
        }
    }

    private fun FirWhenExpression.replaceReturnTypeIfNotExhaustive(): FirWhenExpression {
        if (!isExhaustive) {
            resultType = resultType.resolvedTypeFromPrototype(session.builtinTypes.unitType.type)
        }
        return this
    }

    private fun FirWhenExpression.isOneBranch(): Boolean {
        if (branches.size == 1) return true
        if (branches.size > 2) return false
        val lastBranch = branches.last()
        return lastBranch.condition is FirElseIfTrueCondition && lastBranch.result is FirEmptyExpressionBlock
    }

    override fun transformWhenBranch(whenBranch: FirWhenBranch, data: ResolutionMode): CompositeTransformResult<FirWhenBranch> {
        return whenBranch.also { dataFlowAnalyzer.enterWhenBranchCondition(whenBranch) }
            .transformCondition(transformer, data).also { dataFlowAnalyzer.exitWhenBranchCondition(it) }
            .transformResult(transformer, data).also { dataFlowAnalyzer.exitWhenBranchResult(it) }
            .compose()
    }

    override fun transformWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        val parentWhen = whenSubjectExpression.whenSubject.whenExpression
        val subjectType = parentWhen.subject?.resultType ?: parentWhen.subjectVariable?.returnTypeRef
        if (subjectType != null) {
            whenSubjectExpression.resultType = subjectType
        }
        return whenSubjectExpression.compose()
    }

    // ------------------------------- Try/catch expressions -------------------------------

    override fun transformTryExpression(tryExpression: FirTryExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (tryExpression.calleeReference is FirResolvedNamedReference && tryExpression.resultType !is FirImplicitTypeRef) {
            return tryExpression.compose()
        }

        dataFlowAnalyzer.enterTryExpression(tryExpression)
        tryExpression.transformTryBlock(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitTryMainBlock(tryExpression)
        tryExpression.transformCatches(this, ResolutionMode.ContextDependent)

        @Suppress("NAME_SHADOWING")
        var result = syntheticCallGenerator.generateCalleeForTryExpression(tryExpression)?.let {
            val expectedTypeRef = data.expectedType
            callCompleter.completeCall(it, expectedTypeRef)
        } ?: run {
            tryExpression.resultType = FirErrorTypeRefImpl(null, FirSimpleDiagnostic("Can't resolve try expression", DiagnosticKind.InferenceError))
            tryExpression
        }

        result = if (result.finallyBlock != null) {
            result.also(dataFlowAnalyzer::enterFinallyBlock)
                .transformFinallyBlock(transformer, ResolutionMode.ContextIndependent)
                .also(dataFlowAnalyzer::exitFinallyBlock)
        } else {
            result
        }
        dataFlowAnalyzer.exitTryExpression(result)
        return result.compose()
    }

    override fun transformCatch(catch: FirCatch, data: ResolutionMode): CompositeTransformResult<FirCatch> {
        dataFlowAnalyzer.enterCatchClause(catch)
        catch.parameter.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
        return withScopeCleanup(localScopes) {
            localScopes += FirLocalScope()
            catch.transformParameter(transformer, ResolutionMode.ContextIndependent)
            catch.transformBlock(transformer, ResolutionMode.ContextDependent)
        }.also { dataFlowAnalyzer.exitCatchClause(it) }.compose()
    }

    // ------------------------------- Jumps -------------------------------

    override fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        val result = transformer.transformExpression(jump, data)
        dataFlowAnalyzer.exitJump(jump)
        return result
    }

    override fun transformThrowExpression(throwExpression: FirThrowExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return transformer.transformExpression(throwExpression, data).also {
            dataFlowAnalyzer.exitThrowExceptionNode(it.single as FirThrowExpression)
        }
    }
}
