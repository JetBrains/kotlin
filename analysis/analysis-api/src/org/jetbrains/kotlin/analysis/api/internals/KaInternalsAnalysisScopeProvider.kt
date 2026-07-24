/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsAnalysisScopeProvider {
    public val analysisScope: GlobalSearchScope

    public fun canBeAnalysed(element: PsiElement): Boolean

    /**
     * The implementation of [canBeAnalysed] without the validity assertion check.
     *
     * @see canBeAnalysed
     */
    public fun canBeAnalysedImpl(element: PsiElement): Boolean
}
