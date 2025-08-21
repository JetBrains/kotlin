/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaUsualClassTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaUsualClassType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS : KaUsualClassTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaUsualClassType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, printer) },
                    {
                        typeRenderer.classIdRenderer.renderClassTypeQualifier(analysisSession, type, type.qualifiers, typeRenderer, printer)
                        with(analysisSession) {
                            if (type.isMarkedNullable) {
                                append('?')
                            }
                        }
                    },
                )
            }
        }
    }
}
