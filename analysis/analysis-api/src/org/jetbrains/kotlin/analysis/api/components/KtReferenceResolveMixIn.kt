/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtReference

public interface KtReferenceResolveMixIn : KtAnalysisSessionMixIn {
    public fun KtReference.resolveToSymbols(): Collection<KtSymbol> = withValidityAssertion {
        return analysisSession.referenceResolveProvider.resolveToSymbols(this)
    }

    public fun KtReference.resolveToSymbol(): KtSymbol? = withValidityAssertion {
        return resolveToSymbols().singleOrNull()
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