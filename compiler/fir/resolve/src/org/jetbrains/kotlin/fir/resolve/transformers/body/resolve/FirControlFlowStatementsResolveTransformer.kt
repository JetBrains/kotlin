/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.transformers.FirSyntheticCallGenerator
import org.jetbrains.kotlin.fir.resolve.transformers.FirWhenExhaustivenessTransformer
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
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
        // Do-while has a specific scope structure (its block and condition effectively share the scope)
        return withNewLocalScope {
            doWhileLoop.also(dataFlowAnalyzer::enterDoWhileLoop)
                .apply {
                    transformer.expressionsTransformer.transformBlockInCurrentScope(block, data)
                    dataFlowAnalyzer
                }
                .also(dataFlowAnalyzer::enterDoWhileLoopCondition).transformCondition(transformer, data)
                .also(dataFlowAnalyzer::exitDoWhileLoop)
                .transformOtherChildren(transformer, data).compose()
        }
    }

    // ------------------------------- When expressions -------------------------------

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (whenExpression.calleeReference is FirResolvedNamedReference && whenExpression.resultType !is FirImplicitTypeRef) {
            return whenExpression.compose()
        }
        whenExpression.annotations.forEach { it.accept(this, data) }
        dataFlowAnalyzer.enterWhenExpression(whenExpression)
        return withLocalScopeCleanup with@{
            if (whenExpression.subjectVariable != null) {
                addNewLocalScope()
            }
            @Suppress("NAME_SHADOWING")
            var whenExpression = whenExpression.transformSubject(transformer, ResolutionMode.ContextIndependent)

            val callCompleted = when {
                whenExpression.branches.isEmpty() -> true
                whenExpression.isOneBranch() -> {
                    whenExpression = whenExpression.transformBranches(transformer, ResolutionMode.ContextIndependent)
                    whenExpression.resultType = whenExpression.branches.first().result.resultType
                    true
                }
                else -> {
                    whenExpression = whenExpression.transformBranches(transformer, ResolutionMode.ContextDependent)

                    whenExpression = syntheticCallGenerator.generateCalleeForWhenExpression(whenExpression) ?: run {
                        whenExpression = whenExpression.transformSingle(whenExhaustivenessTransformer, null)
                        dataFlowAnalyzer.exitWhenExpression(whenExpression, callCompleted = true)
                        whenExpression.resultType = buildErrorTypeRef {
                            diagnostic = ConeSimpleDiagnostic("Can't resolve when expression", DiagnosticKind.InferenceError)
                        }
                        return@with whenExpression.compose()
                    }

                    val expectedTypeRef = data.expectedType
                    val completionResult = callCompleter.completeCall(whenExpression, expectedTypeRef)
                    whenExpression = completionResult.result
                    completionResult.callCompleted
                }
            }
            whenExpression = whenExpression.transformSingle(whenExhaustivenessTransformer, null)
            dataFlowAnalyzer.exitWhenExpression(whenExpression, callCompleted)
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
        return lastBranch.source != null && lastBranch.condition is FirElseIfTrueCondition && lastBranch.result is FirEmptyExpressionBlock
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
        val parentWhen = whenSubjectExpression.whenRef.value
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

        tryExpression.annotations.forEach { it.accept(this, data) }
        dataFlowAnalyzer.enterTryExpression(tryExpression)
        tryExpression.transformTryBlock(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitTryMainBlock(tryExpression)
        tryExpression.transformCatches(this, ResolutionMode.ContextDependent)

        var callCompleted = false

        @Suppress("NAME_SHADOWING")
        var result = syntheticCallGenerator.generateCalleeForTryExpression(tryExpression)?.let {
            val expectedTypeRef = data.expectedType
            val completionResult = callCompleter.completeCall(it, expectedTypeRef)
            callCompleted = completionResult.callCompleted
            completionResult.result
        } ?: run {
            tryExpression.resultType = buildErrorTypeRef {
                diagnostic = ConeSimpleDiagnostic("Can't resolve try expression", DiagnosticKind.InferenceError)
            }
            callCompleted = true
            tryExpression
        }

        result = if (result.finallyBlock != null) {
            result.also { dataFlowAnalyzer.enterFinallyBlock() }
                .transformFinallyBlock(transformer, ResolutionMode.ContextIndependent)
                .also(dataFlowAnalyzer::exitFinallyBlock)
        } else {
            result
        }
        dataFlowAnalyzer.exitTryExpression(callCompleted)
        return result.compose()
    }

    override fun transformCatch(catch: FirCatch, data: ResolutionMode): CompositeTransformResult<FirCatch> {
        dataFlowAnalyzer.enterCatchClause(catch)
        catch.parameter.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
        return withNewLocalScope {
            catch.transformParameter(transformer, ResolutionMode.ContextIndependent)
            catch.transformBlock(transformer, ResolutionMode.ContextDependent)
        }.also { dataFlowAnalyzer.exitCatchClause(it) }.compose()
    }

    // ------------------------------- Jumps -------------------------------

    override fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        val expectedTypeRef = (jump as? FirReturnExpression)?.target?.labeledElement?.returnTypeRef

        val mode = if (expectedTypeRef != null) {
            ResolutionMode.WithExpectedType(expectedTypeRef)
        } else {
            ResolutionMode.ContextIndependent
        }
        var result = transformer.transformExpression(jump, mode).single
        if (result is FirReturnExpression) {
            result = result.transformResult(integerLiteralTypeApproximator, expectedTypeRef!!.coneTypeSafe())
        }
        dataFlowAnalyzer.exitJump(jump)
        return result.compose()
    }

    override fun transformThrowExpression(
        throwExpression: FirThrowExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return transformer.transformExpression(throwExpression, data).also {
            dataFlowAnalyzer.exitThrowExceptionNode(it.single as FirThrowExpression)
        }
    }

    // ------------------------------- Elvis -------------------------------

    override fun transformElvisCall(elvisCall: FirElvisCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (elvisCall.calleeReference is FirResolvedNamedReference) return elvisCall.compose()
        elvisCall.transformAnnotations(transformer, data)
        elvisCall.transformLhs(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitElvisLhs(elvisCall)
        elvisCall.transformRhs(transformer, ResolutionMode.ContextDependent)

        var callCompleted = false
        val result = syntheticCallGenerator.generateCalleeForElvisCall(elvisCall)?.let {
            val completionResult = callCompleter.completeCall(elvisCall, data.expectedType)
            callCompleted = completionResult.callCompleted
            completionResult.result
        } ?: run {
            elvisCall.resultType = buildErrorTypeRef {
                diagnostic = ConeSimpleDiagnostic("Can't resolve ?: operator call", DiagnosticKind.InferenceError)
            }
            callCompleted = true
            elvisCall
        }

        dataFlowAnalyzer.exitElvis(callCompleted)

        return result.compose()
    }
}
