/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaAnalysisScopeProvider : KaLifetimeOwner {
    /**
     * A [GlobalSearchScope] which spans the files that can be analyzed by the current [KaSession].
     *
     * For example, [KaSymbol]s can only be built for declarations which are in the analysis scope.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.session.analysisScope' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "analysisScope",
            imports = [
                "org.jetbrains.kotlin.analysis.api.session.analysisScope",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val analysisScope: GlobalSearchScope

    /**
     * Checks whether the [PsiElement] is inside the [analysisScope].
     *
     * For example, a [KaSymbol] can only be built for this [PsiElement] if it can be analyzed.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.session.canBeAnalysed' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.canBeAnalysed()",
            imports = [
                "org.jetbrains.kotlin.analysis.api.session.canBeAnalysed",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public fun PsiElement.canBeAnalysed(): Boolean
}

/**
 * A [GlobalSearchScope] which spans the files that can be analyzed by the current [KaSession].
 *
 * For example, [KaSymbol]s can only be built for declarations which are in the analysis scope.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.session' endpoint instead.",
    replaceWith = ReplaceWith("analysisScope", "org.jetbrains.kotlin.analysis.api.session.analysisScope"),
    level = DeprecationLevel.HIDDEN,
)
@KaContextParameterApi
context(session: KaSession)
public val analysisScope: GlobalSearchScope
    get() = with(session) { analysisScope }

/**
 * Checks whether the [PsiElement] is inside the [analysisScope].
 *
 * For example, a [KaSymbol] can only be built for this [PsiElement] if it can be analyzed.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.session' endpoint instead.",
    replaceWith = ReplaceWith("this.canBeAnalysed()", "org.jetbrains.kotlin.analysis.api.session.canBeAnalysed"),
    level = DeprecationLevel.HIDDEN,
)
@KaContextParameterApi
context(session: KaSession)
public fun PsiElement.canBeAnalysed(): Boolean {
    return with(session) {
        canBeAnalysed()
    }
}
