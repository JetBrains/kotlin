/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.visibility.KaUseSiteVisibilityChecker
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsVisibilityChecker {
    @KaExperimentalApi
    public fun createUseSiteVisibilityChecker(
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression?,
        position: PsiElement,
    ): KaUseSiteVisibilityChecker

    @KaExperimentalApi
    public fun isVisibleInClass(symbol: KaCallableSymbol, classSymbol: KaClassSymbol): Boolean

    public fun isPublicApi(symbol: KaDeclarationSymbol): Boolean
}
