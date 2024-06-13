/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.impl

import org.jetbrains.kotlin.analysis.api.renderer.base.KaKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.KaDeclarationModifiersRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.*

public object KaDeclarationModifiersRendererForSource {
    public val NO_IMPLICIT_MODIFIERS: KaDeclarationModifiersRenderer = KaDeclarationModifiersRenderer {
        modifierListRenderer = KaModifierListRenderer.AS_LIST
        modifiersSorter = KaModifiersSorter.CANONICAL
        modalityProvider = KaRendererModalityModifierProvider.WITHOUT_IMPLICIT_MODALITY
        visibilityProvider = KaRendererVisibilityModifierProvider.NO_IMPLICIT_VISIBILITY
        otherModifiersProvider = KaRendererOtherModifiersProvider.ALL
        keywordsRenderer = KaKeywordsRenderer.AS_WORD
    }
}

public typealias KtDeclarationModifiersRendererForSource = KaDeclarationModifiersRendererForSource