/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.visibility

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

/**
 * Allows checking if [KaDeclarationSymbol] is visible from the current use-site.
 *
 * [KaUseSiteVisibilityChecker] is created by [createUseSiteVisibilityChecker].
 *
 * [KaUseSiteVisibilityChecker] is designed to be reused. Therefore, if you have multiple candidates to check from the same use-site position,
 * it will be more performant to reuse the same [KaUseSiteVisibilityChecker].
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaUseSiteVisibilityChecker : KaLifetimeOwner {
    /**
     * Checks whether the [candidateSymbol] is visible at the current use-site.
     *
     * @param candidateSymbol The symbol whose visibility is to be checked.
     * @return `true` if the [candidateSymbol] is visible from the current use-site, `false` otherwise.
     */
    @KaExperimentalApi
    public fun isVisible(candidateSymbol: KaDeclarationSymbol): Boolean
}
