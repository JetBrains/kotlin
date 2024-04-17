/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter


public interface KtTypeParameterTypeRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderType(type: KtTypeParameterType, printer: PrettyPrinter)

    public object AS_SOURCE : KtTypeParameterTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtTypeParameterType, printer: PrettyPrinter): Unit = printer {
            " ".separated(
                { annotationsRenderer.renderAnnotations(type, printer) },
                {
                    typeNameRenderer.renderName(type.name, type, printer)
                    if (type.nullability == KtTypeNullability.NULLABLE) {
                        printer.append('?')
                    }
                }
            )

        }
    }
}
