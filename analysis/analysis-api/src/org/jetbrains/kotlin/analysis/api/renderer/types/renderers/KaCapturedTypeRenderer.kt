/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaCapturedTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaCapturedType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object AS_PROJECTION : KaCapturedTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaCapturedType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            typeRenderer.typeProjectionRenderer.renderTypeProjection(analysisSession, type.projection, typeRenderer, printer)
        }
    }

    @KaExperimentalApi
    public object AS_CAPTURED_TYPE_WITH_PROJECTION : KaCapturedTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaCapturedType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("CapturedType(")
            AS_PROJECTION.renderType(analysisSession, type, typeRenderer, printer)
            printer.append(")")
        }
    }
}
