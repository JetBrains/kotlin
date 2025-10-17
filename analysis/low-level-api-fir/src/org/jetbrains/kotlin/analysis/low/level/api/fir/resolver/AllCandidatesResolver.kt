/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolver

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunctionCopy
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.fullyProcessCandidate
import org.jetbrains.kotlin.fir.resolve.calls.tower.FirTowerResolver
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.initialTypeOfCandidate
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.logErrorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

class AllCandidatesResolver(private val firSession: FirSession) {
    private val scopeSession = ScopeSession()

    // This transformer is not intended for actual transformations and created here only to simplify access to resolve components
    private val stubBodyResolveTransformer = FirBodyResolveTransformer(
        session = firSession,
        phase = FirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
    )

    private val bodyResolveComponents = object : StubBodyResolveTransformerComponents(
        firSession,
        scopeSession,
        stubBodyResolveTransformer,
        stubBodyResolveTransformer.context,
    ) {
        val collector = AllCandidatesCollector(this, resolutionStageRunner)
        val towerResolver = FirTowerResolver(this, resolutionStageRunner, collector)
        override val callResolver = FirCallResolver(this, towerResolver)

        init {
            callResolver.initTransformer(FirExpressionsResolveTransformer(stubBodyResolveTransformer))
        }
    }

    private val resolutionContext = ResolutionContext(firSession, bodyResolveComponents, bodyResolveComponents.transformer.context)

    fun getAllCandidates(
        resolutionFacade: LLResolutionFacade,
        qualifiedAccess: FirQualifiedAccessExpression,
        calleeName: Name,
        element: KtElement,
        resolutionMode: ResolutionMode,
    ): List<OverloadCandidate> {
        initializeBodyResolveContext(resolutionFacade, element)

        val copiedAccess = copyQualifiedAccess(qualifiedAccess, element) ?: return emptyList()
        return run {
            bodyResolveComponents.callResolver
                .collectAllCandidates(
                    copiedAccess,
                    calleeName,
                    bodyResolveComponents.context.containers,
                    resolutionContext,
                    resolutionMode,
                )
                .apply { postProcessCandidates(copiedAccess) }
        }
    }

    fun getAllCandidatesForDelegatedConstructor(
        resolutionFacade: LLResolutionFacade,
        delegatedConstructorCall: FirDelegatedConstructorCall,
        derivedClassLookupTag: ConeClassLikeLookupTag,
        element: KtElement
    ): List<OverloadCandidate> {
        initializeBodyResolveContext(resolutionFacade, element)

        val constructedType = delegatedConstructorCall.constructedTypeRef.coneType as ConeClassLikeType
        return run {
            val callInfo = bodyResolveComponents.callResolver.callInfoForDelegatingConstructorCall(
                delegatedConstructorCall,
                constructedType,
            )

            with(bodyResolveComponents.towerResolver) {
                reset()
                runResolverForDelegatingConstructor(callInfo, constructedType, derivedClassLookupTag, resolutionContext)
            }

            bodyResolveComponents.collector.allCandidates
                .map { OverloadCandidate(it, isInBestCandidates = it in bodyResolveComponents.collector.bestCandidates()) }
                .apply { postProcessCandidates(delegatedConstructorCall) }
        }
    }

    @OptIn(PrivateForInline::class, SymbolInternals::class)
    private fun initializeBodyResolveContext(resolutionFacade: LLResolutionFacade, element: KtElement) {
        val firFile = element.containingKtFile.getOrBuildFirFile(resolutionFacade)

        // Set up needed context to get all candidates.
        val towerContext = ContextCollector.process(resolutionFacade, firFile, element)?.towerDataContext
        towerContext?.let { bodyResolveComponents.context.replaceTowerDataContext(it) }
        val containingDeclarations =
            element.parentsOfType<KtDeclaration>().map { it.resolveToFirSymbol(resolutionFacade).fir }.toList().asReversed()
        bodyResolveComponents.context.containers.addAll(containingDeclarations)

        // `towerContext` from above should already contain all the scopes for the file,
        // so we just set it manually without calling `withFile`
        bodyResolveComponents.context.file = firFile
    }

    private fun <T> List<OverloadCandidate>.postProcessCandidates(call: T) where T : FirExpression, T : FirResolvable {
        val callCompleter = bodyResolveComponents.callCompleter
        val analyzer = callCompleter.createPostponedArgumentsAnalyzer(resolutionContext)
        val components = resolutionContext.bodyResolveComponents

        forEach { overloadCandidate ->
            val candidate = overloadCandidate.candidate

            // Runs resolution stages. In particular, this action initiates type constraints
            components.resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)

            // Runs completion for the candidate. This step is required to solve the constraint system
            callCompleter.runCompletionForCall(
                candidate = candidate,
                completionMode = ConstraintSystemCompletionMode.FULL,
                call = call,
                initialType = components.initialTypeOfCandidate(candidate),
                analyzer = analyzer,
            )

            overloadCandidate.preserveCalleeInapplicability()
        }
    }

    /**
     * Post-processes a candidate to carry the callee's inapplicability over into the candidate. Without this post-processing, an issue may
     * arise where [getAllCandidates] produces "applicable" candidates with inapplicable callee references.
     *
     * For example, a function call `generic<String, String>` of function `fun <A, B, C> generic() { }` is correctly marked as inapplicable
     * by the compiler (due to the missing type argument), but the `firFile` built during [getAllCandidates] will contain an inapplicable
     * function call `generic<String, String, ERROR>` (with the missing type argument inferred as an error type). The *subsequent*
     * resolution by `bodyResolveComponents.callResolver.collectAllCandidates` feeds this call to
     * [org.jetbrains.kotlin.fir.resolve.calls.CandidateFactory], which doesn't make any guarantees for inapplicable calls. Hence, the
     * resulting candidate is *not* marked as inapplicable and needs to be post-processed.
     */
    private fun OverloadCandidate.preserveCalleeInapplicability() {
        val callSite = candidate.callInfo.callSite
        val calleeReference = callSite.toReference(firSession) as? FirDiagnosticHolder ?: return
        val diagnostic = calleeReference.diagnostic as? ConeInapplicableCandidateError ?: return
        if (diagnostic.applicability != CandidateApplicability.INAPPLICABLE) return

        candidate.addDiagnostic(InapplicableCandidate)
    }
}

/**
 * The passed [qualifiedAccess] is copied to avoid modification of the original tree.
 *
 * The copied tree is then passed to the [org.jetbrains.kotlin.fir.resolve.calls.overloads.FirOverloadByLambdaReturnTypeResolver]
 * which may modify the tree. In particular, it may change the callee reference and lambdas.
 *
 * There is no goal to make a proper deep copy of the subtree – it is enough to cover the known cases there
 * the modification is possible.
 */
private fun copyQualifiedAccess(
    qualifiedAccess: FirQualifiedAccessExpression,
    element: KtElement,
): FirQualifiedAccessExpression? = when (qualifiedAccess) {
    is FirFunctionCall -> buildFunctionCallCopy(qualifiedAccess) {
        argumentList = when (val argumentListToCopy = qualifiedAccess.argumentList) {
            is FirEmptyArgumentList -> argumentListToCopy
            is FirResolvedArgumentList -> {
                val newMapping = argumentListToCopy.mapping.mapKeysTo(LinkedHashMap()) { copyArgument(it.key) }

                /**
                 * Arguments from the original argument list are used, so it has to be copied as well.
                 * This usage can be found in [org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo.arguments]
                 * and was introduced in KT-66124
                 */
                val originalArgumentList = argumentListToCopy.originalArgumentList
                val newOriginalList = if (originalArgumentList != null) {
                    buildArgumentListCopy(originalArgumentList) {
                        arguments.replaceAll(::copyArgument)
                    }
                } else {
                    null
                }

                buildResolvedArgumentList(
                    original = newOriginalList,
                    mapping = newMapping,
                )
            }

            else -> {
                logger<AllCandidatesResolver>().logErrorWithAttachment("Unexpected argument list ${argumentListToCopy::class.simpleName}") {
                    withFirEntry("argumentList", argumentListToCopy)
                    withPsiEntry("psi", element)
                }

                return null
            }
        }
    }
    is FirPropertyAccessExpression -> buildPropertyAccessExpressionCopy(qualifiedAccess) {}
    else -> {
        logger<AllCandidatesResolver>().logErrorWithAttachment("Unsupported qualified access ${qualifiedAccess::class.simpleName}") {
            withFirEntry("qualifiedAccess", qualifiedAccess)
            withPsiEntry("psi", element)
        }

        null
    }
}

private fun copyArgument(argument: FirExpression): FirExpression = when (argument) {
    is FirWrappedArgumentExpression -> {
        val newExpression = copyArgument(argument.expression)
        when (argument) {
            is FirNamedArgumentExpression -> buildNamedArgumentExpressionCopy(argument) { expression = newExpression }
            is FirSpreadArgumentExpression -> buildSpreadArgumentExpressionCopy(argument) { expression = newExpression }
        }
    }
    is FirAnonymousFunctionExpression -> {
        buildAnonymousFunctionExpressionCopy(argument) {
            anonymousFunction = buildAnonymousFunctionCopy(argument.anonymousFunction) { symbol = FirAnonymousFunctionSymbol() }
        }
    }
    else -> argument
}