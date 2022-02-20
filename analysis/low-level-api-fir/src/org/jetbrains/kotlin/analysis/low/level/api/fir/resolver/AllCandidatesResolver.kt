/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolver

import org.jetbrains.kotlin.analysis.api.impl.barebone.parentsOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.OverloadCandidate
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

class AllCandidatesResolver(firSession: FirSession) {
    private val bodyResolveComponents = createStubBodyResolveComponents(firSession)
    private val resolutionContext = ResolutionContext(firSession, bodyResolveComponents, bodyResolveComponents.transformer.context)

    fun getAllCandidates(
        resolveState: LLFirModuleResolveState,
        functionCall: FirFunctionCall,
        calleeName: Name,
        element: KtElement
    ): List<OverloadCandidate> {
        initializeBodyResolveContext(resolveState, element)

        val firFile = element.containingKtFile.getOrBuildFirFile(resolveState)
        return bodyResolveComponents.context.withFile(firFile, bodyResolveComponents) {
            bodyResolveComponents.callResolver.collectAllCandidates(
                functionCall,
                calleeName,
                bodyResolveComponents.context.containers,
                resolutionContext
            )
        }
    }

    @OptIn(PrivateForInline::class, SymbolInternals::class)
    private fun initializeBodyResolveContext(resolveState: LLFirModuleResolveState, element: KtElement) {
        // Set up needed context to get all candidates.
        val towerContext = resolveState.getTowerContextProvider(element.containingKtFile).getClosestAvailableParentContext(element)
        towerContext?.let { bodyResolveComponents.context.replaceTowerDataContext(it) }
        val containingDeclarations =
            element.parentsOfType<KtDeclaration>().map { it.resolveToFirSymbol(resolveState).fir }.toList().asReversed()
        bodyResolveComponents.context.containers.addAll(containingDeclarations)
    }
}