/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtDynamicType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens


public interface KtDynamicTypeRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderType(type: KtDynamicType, printer: PrettyPrinter)

    public object AS_DYNAMIC_WORD : KtDynamicTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtDynamicType, printer: PrettyPrinter) {
            keywordsRenderer.renderKeyword(KtTokens.DYNAMIC_KEYWORD, type, printer)
        }
    }
}
