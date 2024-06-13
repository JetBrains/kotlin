/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtReference

@OptIn(KaAnalysisApiInternals::class, KaAnalysisNonPublicApi::class)
public interface KaReferenceResolveMixIn : KaSessionMixIn {
    public fun KtReference.resolveToSymbols(): Collection<KaSymbol> = withValidityAssertion {
        return analysisSession.resolver.resolveToSymbols(this)
    }

    public fun KtReference.resolveToSymbol(): KaSymbol? = withValidityAssertion {
        return analysisSession.resolver.resolveToSymbols(this).singleOrNull()
    }

    /**
     * Checks if the reference is an implicit reference to a companion object via the containing class.
     *
     * Example:
     * ```
     * class A {
     *    companion object {
     *       fun foo() {}
     *    }
     * }
     * ```
     *
     * For the case provided, inside the call `A.foo()`,
     * the `A` is an implicit reference to the companion object, so `isImplicitReferenceToCompanion` returns `true`
     *
     * @return `true` if the reference is an implicit reference to a companion object, `false` otherwise.
     */
    public fun KtReference.isImplicitReferenceToCompanion(): Boolean = withValidityAssertion {
        analysisSession.referenceResolveProvider.isImplicitReferenceToCompanion(this)
    }
}

public typealias KtReferenceResolveMixIn = KaReferenceResolveMixIn