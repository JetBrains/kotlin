/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.idea.frontend.api.components.KtSamResolver
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.getClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSamConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken

internal class KtFirSamResolver(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSamResolver(), KtFirAnalysisSessionComponent {

    override fun getSamConstructor(ktClassLikeSymbol: KtClassLikeSymbol): KtSamConstructorSymbol? {
        val classId = ktClassLikeSymbol.classIdIfNonLocal ?: return null
        val owner = analysisSession.getClassLikeSymbol(classId) as? FirRegularClass ?: return null
        val resolver = LocalSamResolver(analysisSession.rootModuleSession)
        return resolver.getSamConstructor(owner)?.let {
            analysisSession.firSymbolBuilder.functionLikeBuilder.buildSamConstructorSymbol(it)
        }
    }

    private class LocalSamResolver(
        private val firSession: FirSession,
    ) {
        private val scopeSession = ScopeSession()

        // TODO: This transformer is not intended for actual transformations and
        //  created here only to simplify access to SAM resolver in body resolve components
        private val stubBodyResolveTransformer = object : FirBodyResolveTransformer(
            session = firSession,
            phase = FirResolvePhase.BODY_RESOLVE,
            implicitTypeOnly = false,
            scopeSession = scopeSession,
        ) {}

        private val bodyResolveComponents =
            FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents(
                firSession,
                scopeSession,
                stubBodyResolveTransformer,
                stubBodyResolveTransformer.context,
            )

        // TODO: this doesn't guarantee that the same synthetic function (as a SAM constructor) is created/returned
        fun getSamConstructor(firClass: FirRegularClass): FirSimpleFunction? {
            val samConstructor = bodyResolveComponents.samResolver.getSamConstructor(firClass) ?: return null
            if (samConstructor.origin != FirDeclarationOrigin.SamConstructor) return null
            return samConstructor
        }
    }
}
