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
    public fun renderTypeProjection(
        analysisSession: KtAnalysisSession,
        projection: KtTypeProjection,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_VARIANCE : KtTypeProjectionRenderer {
        override fun renderTypeProjection(
            analysisSession: KtAnalysisSession,
            projection: KtTypeProjection,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                when (projection) {
                    is KtStarTypeProjection -> printer.append('*')
                    is KtTypeArgumentWithVariance -> {
                        " ".separated(
                            { printer.append(projection.variance.label) },
                            { typeRenderer.renderType(analysisSession, projection.type, printer) },
                        )
                    }
                }
            }
        }
    }

    public object WITHOUT_VARIANCE : KtTypeProjectionRenderer {
        override fun renderTypeProjection(
            analysisSession: KtAnalysisSession,
            projection: KtTypeProjection,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            when (projection) {
                is KtStarTypeProjection -> printer.append('*')
                is KtTypeArgumentWithVariance -> {
                    typeRenderer.renderType(analysisSession, projection.type, printer)
                }
            }
        }
    }
}

