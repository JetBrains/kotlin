/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtIntersectionType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter


public interface KtIntersectionTypeRenderer {
    public fun renderType(
        analysisSession: KtAnalysisSession,
        type: KtIntersectionType,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_INTERSECTION : KtIntersectionTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtIntersectionType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                printCollection(type.conjuncts, separator = " & ") {
                    typeRenderer.renderType(analysisSession, it, printer)
                }
            }
        }
    }

}
