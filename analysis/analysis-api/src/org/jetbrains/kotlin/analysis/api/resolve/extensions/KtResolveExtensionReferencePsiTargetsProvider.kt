/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolve.extensions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol

public abstract class KtResolveExtensionReferencePsiTargetsProvider {
    /**
     * Provides a [PsiElement] where `reference.resolveTo` will lead for a [symbol]
     *
     * Usually returns a single result. Might return an empty collection if there is no navigation target.
     * Also, might multiple targets in a case of ambiguity or multiple targets for a [symbol]
     *
     * Returned [PsiElement] will be used as a navigation target for a reference inside the IDE.
     */
    public abstract fun KtAnalysisSession.getReferenceTargetsForSymbol(symbol: KtSymbol): Collection<PsiElement>
}