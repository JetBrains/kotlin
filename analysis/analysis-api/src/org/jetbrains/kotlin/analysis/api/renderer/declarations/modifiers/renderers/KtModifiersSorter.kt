/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.addRemoveModifier.sortModifiers

public interface KtModifiersSorter {
    public fun sort(
        analysisSession: KtAnalysisSession,
        modifiers: List<KtModifierKeywordToken>,
        owner: KtDeclarationSymbol,
    ): List<KtModifierKeywordToken>

    public object CANONICAL : KtModifiersSorter {
        override fun sort(
            analysisSession: KtAnalysisSession,
            modifiers: List<KtModifierKeywordToken>,
            owner: KtDeclarationSymbol,
        ): List<KtModifierKeywordToken> {
            return sortModifiers(modifiers)
        }
    }
}