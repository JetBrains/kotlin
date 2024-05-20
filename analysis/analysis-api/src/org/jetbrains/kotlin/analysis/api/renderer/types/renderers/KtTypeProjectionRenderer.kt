/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KaTypeProjectionRenderer {
    public fun renderTypeProjection(
        analysisSession: KaSession,
        projection: KaTypeProjection,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_VARIANCE : KaTypeProjectionRenderer {
        override fun renderTypeProjection(
            analysisSession: KaSession,
            projection: KaTypeProjection,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                when (projection) {
                    is KaStarTypeProjection -> printer.append('*')
                    is KaTypeArgumentWithVariance -> {
                        " ".separated(
                            { printer.append(projection.variance.label) },
                            { typeRenderer.renderType(analysisSession, projection.type, printer) },
                        )
                    }
                }
            }
        }
    }

    public object WITHOUT_VARIANCE : KaTypeProjectionRenderer {
        override fun renderTypeProjection(
            analysisSession: KaSession,
            projection: KaTypeProjection,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            when (projection) {
                is KaStarTypeProjection -> printer.append('*')
                is KaTypeArgumentWithVariance -> {
                    typeRenderer.renderType(analysisSession, projection.type, printer)
                }
            }
        }
    }
}

public typealias KtTypeProjectionRenderer = KaTypeProjectionRenderer