/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.DirectClassInheritorsResolver
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.utils.addToStdlib.runIf

open class FirBodyResolveTransformer(
    session: FirSession,
    phase: FirResolvePhase,
    implicitTypeOnly: Boolean,
    scopeSession: ScopeSession,
    returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
    outerBodyResolveContext: BodyResolveContext? = null,
) : FirAbstractBodyResolveTransformerDispatcher(
    session,
    phase,
    implicitTypeOnly,
    scopeSession,
    returnTypeCalculator,
    outerBodyResolveContext,
    expandTypeAliases = true,
) {
    final override val expressionsTransformer: FirExpressionsResolveTransformer = FirExpressionsResolveTransformer(this)
    final override val declarationsTransformer: FirDeclarationsResolveTransformer = FirDeclarationsResolveTransformer(this)

    private val directClassInheritorsResolver: DirectClassInheritorsResolver? =
        runIf(LanguageFeature.DirectClassInheritors.isEnabled()) {
            DirectClassInheritorsResolver(session)
        }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirRegularClass =
        super.transformRegularClass(regularClass, data).apply { directClassInheritorsResolver?.resolveRegularClass(this) }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias =
        super.transformTypeAlias(typeAlias, data).apply { directClassInheritorsResolver?.resolveTypeAlias(this) }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: ResolutionMode): FirAnonymousObject =
        super.transformAnonymousObject(anonymousObject, data).apply { directClassInheritorsResolver?.resolveAnonymousObject(this) }
}
