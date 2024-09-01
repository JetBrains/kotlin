/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
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

@KaExperimentalApi
@Deprecated("Use 'KaTypeProjectionRenderer' instead", ReplaceWith("KaTypeProjectionRenderer"))
public typealias KtTypeProjectionRenderer = KaTypeProjectionRenderer