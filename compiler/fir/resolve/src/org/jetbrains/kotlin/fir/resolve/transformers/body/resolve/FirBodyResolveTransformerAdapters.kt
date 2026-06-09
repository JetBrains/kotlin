/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.AdapterForResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping

@OptIn(AdapterForResolveProcessor::class)
class FirBodyResolveProcessor(session: FirSession, scopeSession: ScopeSession) : FirTransformerBasedResolveProcessor(
    session, scopeSession, FirResolvePhase.BODY_RESOLVE
) {
    override val transformer: FirBodyResolveTransformerAdapter = FirBodyResolveTransformerAdapter(session, scopeSession)
}

@AdapterForResolveProcessor
class FirBodyResolveTransformerAdapter(session: FirSession, scopeSession: ScopeSession) : FirTransformer<Any?>() {
    private val transformer = object : FirBodyResolveTransformer(
        session,
        phase = FirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession
    ) {
        override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirRegularClass =
            super.transformRegularClass(regularClass, data).apply { components.directClassInheritorsResolver?.resolveRegularClass(this) }

        override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias =
            super.transformTypeAlias(typeAlias, data).apply { components.directClassInheritorsResolver?.resolveTypeAlias(this) }

        override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: ResolutionMode): FirAnonymousObject =
            super.transformAnonymousObject(anonymousObject, data)
                .apply { components.directClassInheritorsResolver?.resolveAnonymousObject(this) }

        override fun transformNamedFunction(namedFunction: FirNamedFunction, data: ResolutionMode): FirNamedFunction =
            super.transformNamedFunction(namedFunction, data)
                .apply { components.directCallableOverridesResolver?.resolveNamedFunction(this, false) }

        override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty =
            super.transformProperty(property, data).apply { components.directCallableOverridesResolver?.resolveProperty(this, false) }
    }

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        return withFileAnalysisExceptionWrapping(file) {
            file.transform(transformer, ResolutionMode.ContextIndependent)
        }
    }
}
