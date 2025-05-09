/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaTypeParameterTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaTypeParameterType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object AS_SOURCE : KaTypeParameterTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaTypeParameterType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, printer) },
                    {
                        typeRenderer.typeNameRenderer.renderName(analysisSession, type.name, type, typeRenderer, printer)
                        with(analysisSession) {
                            if (type.isMarkedNullable) {
                                printer.append('?')
                            }
                        }
                    }
                )

            }
        }
    }
}
