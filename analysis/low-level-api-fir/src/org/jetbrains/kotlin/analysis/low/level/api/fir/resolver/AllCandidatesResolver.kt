/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolver

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.tower.FirTowerResolver
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

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
        firResolveSession: LLFirResolveSession,
        functionCall: FirFunctionCall,
        calleeName: Name,
        element: KtElement
    ): List<OverloadCandidate> {
        initializeBodyResolveContext(firResolveSession, element)

        val firFile = element.containingKtFile.getOrBuildFirFile(firResolveSession)
        return bodyResolveComponents.context.withFile(firFile, bodyResolveComponents) {
            bodyResolveComponents.callResolver
                .collectAllCandidates(functionCall, calleeName, bodyResolveComponents.context.containers, resolutionContext)
        }
    }

    fun getAllCandidatesForDelegatedConstructor(
        firResolveSession: LLFirResolveSession,
        delegatedConstructorCall: FirDelegatedConstructorCall,
        element: KtElement
    ): List<OverloadCandidate> {
        initializeBodyResolveContext(firResolveSession, element)

        val firFile = element.containingKtFile.getOrBuildFirFile(firResolveSession)
        val constructedType = delegatedConstructorCall.constructedTypeRef.coneType as ConeClassLikeType
        return bodyResolveComponents.context.withFile(firFile, bodyResolveComponents) {
            bodyResolveComponents.callResolver.resolveDelegatingConstructorCall(delegatedConstructorCall, constructedType)
            bodyResolveComponents.collector.allCandidates.map {
                OverloadCandidate(it, isInBestCandidates = it in bodyResolveComponents.collector.bestCandidates())
            }
        }
    }

    @OptIn(PrivateForInline::class, SymbolInternals::class)
    private fun initializeBodyResolveContext(firResolveSession: LLFirResolveSession, element: KtElement) {
        // Set up needed context to get all candidates.
        val towerContext = firResolveSession.getTowerContextProvider(element.containingKtFile).getClosestAvailableParentContext(element)
        towerContext?.let { bodyResolveComponents.context.replaceTowerDataContext(it) }
        val containingDeclarations =
            element.parentsOfType<KtDeclaration>().map { it.resolveToFirSymbol(firResolveSession).fir }.toList().asReversed()
        bodyResolveComponents.context.containers.addAll(containingDeclarations)
    }
}