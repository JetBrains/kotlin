/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.psi.KtElement

class FirModuleResolveStateForCompletion(
    mainState: FirModuleResolveStateImpl
) : FirModuleResolveStateImpl(mainState.sessionProvider) {
    override val cache: PsiToFirCache = PsiToFirCacheForCompletion(mainState.cache)
}

private class PsiToFirCacheForCompletion(private val delegate: PsiToFirCache) : PsiToFirCache() {
    private val cache = mutableMapOf<KtElement, FirElement>()

    override fun get(psi: KtElement): FirElement? =
        cache[psi] ?: delegate[psi]

    override fun set(psi: KtElement, fir: FirElement) {
        cache[psi] = fir
    }

    override fun remove(psi: PsiElement) {
        cache.remove(psi)
    }
}