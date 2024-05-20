/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaDynamicType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens


public interface KaDynamicTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaDynamicType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_DYNAMIC_WORD : KaDynamicTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaDynamicType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            typeRenderer.keywordsRenderer.renderKeyword(analysisSession, KtTokens.DYNAMIC_KEYWORD, type, printer)
        }
    }
}

public typealias KtDynamicTypeRenderer = KaDynamicTypeRenderer