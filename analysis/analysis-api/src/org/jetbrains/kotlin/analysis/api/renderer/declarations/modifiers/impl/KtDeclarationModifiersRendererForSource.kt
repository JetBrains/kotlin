/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.impl

import org.jetbrains.kotlin.analysis.api.renderer.base.KtKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.KtDeclarationModifiersRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.*

public object KtDeclarationModifiersRendererForSource {
    public val NO_IMPLICIT_MODIFIERS: KtDeclarationModifiersRenderer = KtDeclarationModifiersRenderer {
        modifierListRenderer = KtModifierListRenderer.AS_LIST
        modifiersSorter = KtModifiersSorter.CANONICAL
        modalityProvider = KtRendererModalityModifierProvider.WITHOUT_IMPLICIT_MODALITY
        visibilityProvider = KtRendererVisibilityModifierProvider.NO_IMPLICIT_VISIBILITY
        otherModifiersProvider = KtRendererOtherModifiersProvider.ALL
        keywordsRenderer = KtKeywordsRenderer.AS_WORD
    }
}