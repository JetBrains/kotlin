/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtTypeProjectionRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderTypeProjection(projection: KtTypeProjection, printer: PrettyPrinter)

    public object WITH_VARIANCE : KtTypeProjectionRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderTypeProjection(projection: KtTypeProjection, printer: PrettyPrinter): Unit = printer {
            when (projection) {
                is KtStarTypeProjection -> printer.append('*')
                is KtTypeArgumentWithVariance -> {
                    " ".separated(
                        { printer.append(projection.variance.label) },
                        { renderType(projection.type, printer) },
                    )
                }
            }
        }
    }

    public object WITHOUT_VARIANCE : KtTypeProjectionRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderTypeProjection(projection: KtTypeProjection, printer: PrettyPrinter) {
            when (projection) {
                is KtStarTypeProjection -> printer.append('*')
                is KtTypeArgumentWithVariance -> {
                    renderType(projection.type, printer)
                }
            }
        }
    }
}

