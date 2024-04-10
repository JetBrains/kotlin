/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter


public interface KtDefinitelyNotNullTypeRenderer {
    public fun renderType(
        analysisSession: KtAnalysisSession,
        type: KtDefinitelyNotNullType,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_TYPE_INTERSECTION : KtDefinitelyNotNullTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtDefinitelyNotNullType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            with(analysisSession) {
                printer {
                    typeRenderer.renderType(analysisSession, type.original, printer)
                    printer.append(" & ")
                    typeRenderer.renderType(analysisSession, builtinTypes.ANY, printer)
                }
            }
        }
    }

}
