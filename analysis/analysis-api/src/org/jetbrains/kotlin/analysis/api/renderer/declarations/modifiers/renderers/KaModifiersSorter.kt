/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.addRemoveModifier.sortModifiers

@KaExperimentalApi
public interface KaModifiersSorter {
    public fun sort(
        analysisSession: KaSession,
        modifiers: List<KtModifierKeywordToken>,
        owner: KaDeclarationSymbol,
    ): List<KtModifierKeywordToken>

    public object CANONICAL : KaModifiersSorter {
        override fun sort(
            analysisSession: KaSession,
            modifiers: List<KtModifierKeywordToken>,
            owner: KaDeclarationSymbol,
        ): List<KtModifierKeywordToken> {
            return sortModifiers(modifiers)
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaModifiersSorter' instead", ReplaceWith("KaModifiersSorter"))
public typealias KtModifiersSorter = KaModifiersSorter