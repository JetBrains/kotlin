/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.inference.TemporaryInferenceSessionHook
import org.jetbrains.kotlin.fir.resolve.transformers.FirSyntheticCallGenerator
import org.jetbrains.kotlin.fir.resolve.transformers.FirWhenExhaustivenessComputer
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.model.isNullableAny

class FirControlFlowStatementsResolveTransformer(transformer: FirAbstractBodyResolveTransformerDispatcher) :
    FirPartialBodyResolveTransformer(transformer) {

    private val syntheticCallGenerator: FirSyntheticCallGenerator get() = components.syntheticCallGenerator

    // ------------------------------- Loops -------------------------------

    override fun transformWhileLoop(whileLoop: FirWhileLoop, data: ResolutionMode): FirStatement {
        return whileLoop.also(dataFlowAnalyzer::enterWhileLoop)
            .transformCondition(transformer, withExpectedType(session.builtinTypes.booleanType))
            .also(dataFlowAnalyzer::exitWhileLoopCondition)
            .transformBlock(transformer, ResolutionMode.ContextIndependent).also(dataFlowAnalyzer::exitWhileLoop)
            .transformLabel(transformer, ResolutionMode.ContextIndependent)
            .transformAnnotations(transformer, ResolutionMode.ContextIndependent)
    }

    override fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: ResolutionMode): FirStatement {
        // Do-while has a specific scope structure (its block and condition effectively share the scope)
        return context.forBlock(session) {
            doWhileLoop.also(dataFlowAnalyzer::enterDoWhileLoop)
                .also {
                    transformer.expressionsTransformer?.transformBlockInCurrentScope(it.block, ResolutionMode.ContextIndependent)
                }
                .also(dataFlowAnalyzer::enterDoWhileLoopCondition)
                .transformCondition(transformer, withExpectedType(session.builtinTypes.booleanType))
                .also(dataFlowAnalyzer::exitDoWhileLoop)
                .transformLabel(transformer, ResolutionMode.ContextIndependent)
        }.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
    }

    // ------------------------------- When expressions -------------------------------

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: ResolutionMode): FirStatement {
        if (whenExpression.calleeReference is FirResolvedNamedReference && whenExpression.hasResolvedType) {
            return whenExpression
        }
        whenExpression.annotations.forEach { it.accept(this, data) }
        dataFlowAnalyzer.enterWhenExpression(whenExpression)
        return context.withWhenExpression(whenExpression, session) with@{
            @Suppress("NAME_SHADOWING")
            var whenExpression = whenExpression.transformSubjectVariable(transformer, ResolutionMode.ContextIndependent)
            var completionNeeded = false
            when {
                whenExpression.branches.isEmpty() -> {
                    whenExpression.resultType = session.builtinTypes.unitType.coneType
                }
                whenExpression.isOneBranch() && data.forceFullCompletion && data !is ResolutionMode.WithExpectedType -> {
                    whenExpression = whenExpression.transformBranches(transformer, ResolutionMode.ContextIndependent)
                    whenExpression.resultType = whenExpression.branches.first().result.resolvedType
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

                    whenExpression = syntheticCallGenerator.generateCalleeForWhenExpression(
                        whenExpression,
                        resolutionContext,
                        data,
                    )
                    completionNeeded = true
                }
            }
            val exhaustivenessStatus = FirWhenExhaustivenessComputer.computeExhaustivenessStatus(whenExpression, session, context.file)
            whenExpression.replaceExhaustivenessStatus(exhaustivenessStatus)

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
            whenExpression.replaceReturnTypeIfNotExhaustive(session)
            whenExpression
        }
    }

    private fun FirWhenExpression.isOneBranch(): Boolean {
        if (branches.size == 1) return true
        if (branches.size > 2) return false
        val lastBranch = branches.last()
        return lastBranch.source != null && lastBranch.condition is FirElseIfTrueCondition && lastBranch.result is FirEmptyExpressionBlock
    }

    override fun transformWhenBranch(whenBranch: FirWhenBranch, data: ResolutionMode): FirWhenBranch {
        dataFlowAnalyzer.enterWhenBranchCondition(whenBranch)
        return whenBranch.transformCondition(transformer, withExpectedType(session.builtinTypes.booleanType))
            .also { dataFlowAnalyzer.exitWhenBranchCondition(it) }
            .transformResult(transformer, data)
            .also { dataFlowAnalyzer.exitWhenBranchResult(it) }

    }

    override fun transformWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: ResolutionMode
    ): FirStatement {
        dataFlowAnalyzer.exitWhenSubjectExpression(whenSubjectExpression)
        return components.transformExpressionUsingSmartcastInfo(whenSubjectExpression)
    }

    // ------------------------------- Try/catch expressions -------------------------------

    override fun transformTryExpression(tryExpression: FirTryExpression, data: ResolutionMode): FirStatement {
        if (tryExpression.calleeReference is FirResolvedNamedReference && tryExpression.hasResolvedType) {
            return tryExpression
        }

        tryExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        dataFlowAnalyzer.enterTryExpression(tryExpression)
        tryExpression.transformTryBlock(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitTryMainBlock()
        tryExpression.transformCatches(this, ResolutionMode.ContextDependent)

        val incomplete = syntheticCallGenerator.generateCalleeForTryExpression(tryExpression, resolutionContext, data)
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
                ResolutionMode.WithExpectedType(expectedTypeRef)

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

        dataFlowAnalyzer.enterElvis(elvisExpression)

        elvisExpression.transformLhs(
            transformer,
            // should be` expectedType.makeNullable()` or ResolutionMode.ContextDependent since LV >= 2.1
            computeResolutionModeForElvisLHS(data)
        )
        dataFlowAnalyzer.exitElvisLhs(elvisExpression)

        val resolutionModeForRhs = withExpectedType(
            data.expectedType?.takeUnless { it.isNullableAny },
            lastStatementInBlock = (data as? ResolutionMode.WithExpectedType)?.lastStatementInBlock == true
        )
        elvisExpression.transformRhs(
            transformer,
            resolutionModeForRhs
        )

        val result = callCompleter.completeCall(
            syntheticCallGenerator.generateCalleeForElvisExpression(elvisExpression, resolutionContext, data),
            if (data is ResolutionMode.WithExpectedType) resolutionModeForRhs else data
        )

        var isLhsNotNull = false

        // TODO: This whole `if` should be probably removed once we get rid of seemingly redundant
        //  @Exact annotation on the return type of the synthetic function, see KT-55692
        // TODO: Check if the type of the RHS being null can lead to a bug, see KT-61837
        @OptIn(UnresolvedExpressionTypeAccess::class)
        if (result.rhs.coneTypeOrNull?.isNothing == true) {
            val lhsType = result.lhs.resolvedType
            // Converting to non-raw type is necessary to preserver the K1 semantics (see KT-54526)
            val newReturnType =
                lhsType.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext)
                    .convertToNonRawVersion()
            result.replaceConeTypeOrNull(newReturnType)

            // For regularly resolved synthetic call, this hook is being called on the whole expression,
            // thus correctly substituting necessary (fixed or fixed-on-demand) type variables.
            // But it's not expected to do that on the arguments of such calls,
            // so in `lhsType` (which above is being transferred to `result`) there might be some type variables left.
            @OptIn(TemporaryInferenceSessionHook::class)
            context.inferenceSession.updateExpressionReturnTypeWithCurrentSubstitutorInPCLA(result, data)

            isLhsNotNull = true
        }

        session.typeContext.run {
            // If the result type is a type variable type, we're part of a call argument.
            // In that case, we could have an unresolved lambda argument on the RHS which will lead to an exception in
            // result.rhs.resolvedType.
            // Also, we don't want to turn type variables into DNNs because it can lead to contradictions in the constraint system.
            // compiler/testData/diagnostics/tests/controlStructures/lambdasInExclExclAndElvis.kt breaks otherwise.
            if (result.resolvedType.let { it !is ConeTypeVariableType && it.isNullableType() }) {
                val rhsResolvedType = result.rhs.resolvedType
                // This part of the code is a kind of workaround, and it probably will be resolved by KT-55692
                if (!rhsResolvedType.refinedTypeForDataFlowOrSelf.isNullableType()) {
                    // It's definitely not a flexible with nullable bound
                    // Sometimes return type for special call for elvis operator might be nullable,
                    // but result is not nullable if the right type is not nullable
                    result.replaceConeTypeOrNull(result.resolvedType.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext))
                } else if (isFlexibleWithNotNullable(rhsResolvedType.refinedTypeForDataFlowOrSelf)) {
                    result.replaceConeTypeOrNull(result.resultType.makeConeFlexibleTypeWithNotNullableLowerBound(session.typeContext))
                }
            }
        }

        dataFlowAnalyzer.exitElvis(elvisExpression, isLhsNotNull, data.forceFullCompletion)
        return result
    }

    private fun ConeInferenceContext.isFlexibleWithNotNullable(rhsResolvedType: ConeKotlinType): Boolean =
        rhsResolvedType is ConeFlexibleType && !rhsResolvedType.lowerBound.isNullableType()

    private fun computeResolutionModeForElvisLHS(
        data: ResolutionMode,
    ): ResolutionMode {
        val expectedType = data.expectedType?.takeUnless { it.isNullableAny }
        val lastStatementInBlock = (data as? ResolutionMode.WithExpectedType)?.lastStatementInBlock == true

        val isObsoleteCompilerMode =
            !LanguageFeature.ElvisInferenceImprovementsIn21.isEnabled()
        return when {
            lastStatementInBlock && expectedType?.isUnitOrFlexibleUnit == true ->
                when {
                    isObsoleteCompilerMode ->
                        // The problematic part is that we even forget about nullability
                        // And forcefully run coercion to Unit of nullable LHS
                        withExpectedType(
                            expectedType, // Always Unit here
                            lastStatementInBlock = true
                        )
                    else -> ResolutionMode.ContextDependent
                }
            // In general, it might be ResolutionMode.ContextDependent as in the Unit-case with modern LV.
            // The expected type should be applied at the completion stage for the whole elvis-call, thus affecting the inference
            // at the LHS, too.
            //
            // But in some situations (like KT-72996 and KT-73011 as its non-elvis generalized version) propagation of the expected type
            // helps to resolve callable references properly, so at this point we can't just get it back.
            else -> withExpectedType(expectedType?.withNullability(nullable = true, session.typeContext))
        }
    }

    private fun ConeKotlinType.makeConeFlexibleTypeWithNotNullableLowerBound(typeContext: ConeTypeContext): ConeKotlinType {
        with(typeContext) {
            return when (this@makeConeFlexibleTypeWithNotNullableLowerBound) {
                is ConeDefinitelyNotNullType ->
                    error("It can't happen because of the previous `isNullableType` check")
                is ConeFlexibleType -> {
                    if (!lowerBound.isNullableType()) {
                        this@makeConeFlexibleTypeWithNotNullableLowerBound
                    } else {
                        ConeFlexibleType(
                            lowerBound.makeConeTypeDefinitelyNotNullOrNotNull(typeContext),
                            upperBound,
                            isTrivial = false,
                        )
                    }
                }
                is ConeIntersectionType -> ConeIntersectionType(
                    intersectedTypes.map { it.makeConeFlexibleTypeWithNotNullableLowerBound(typeContext) }
                )
                is ConeRigidType -> ConeFlexibleType(
                    makeConeTypeDefinitelyNotNullOrNotNull(typeContext),
                    this@makeConeFlexibleTypeWithNotNullableLowerBound,
                    isTrivial = false,
                )
            }
        }
    }
}
