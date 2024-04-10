/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtCapturedType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter


public interface KtCapturedTypeRenderer {
    public fun renderType(
        analysisSession: KtAnalysisSession,
        type: KtCapturedType,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_PROJECTION : KtCapturedTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtCapturedType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            typeRenderer.typeProjectionRenderer.renderTypeProjection(analysisSession, type.projection, typeRenderer, printer)
        }
    }

    public object AS_CAPTURED_TYPE_WITH_PROJECTION : KtCapturedTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtCapturedType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("CapturedType(")
            AS_PROJECTION.renderType(analysisSession, type, typeRenderer, printer)
            printer.append(")")
        }
    }
}
