/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KtUsualClassType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter


public interface KtUsualClassTypeRenderer {
    public fun renderType(
        analysisSession: KtAnalysisSession,
        type: KtUsualClassType,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS : KtUsualClassTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtUsualClassType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, printer) },
                    {
                        typeRenderer.classIdRenderer.renderClassTypeQualifier(analysisSession, type, typeRenderer, printer)
                        if (type.nullability == KtTypeNullability.NULLABLE) {
                            append('?')
                        }
                    },
                )
            }
        }
    }
}
