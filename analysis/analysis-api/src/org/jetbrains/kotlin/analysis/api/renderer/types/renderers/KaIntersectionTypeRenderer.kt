/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaIntersectionTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaIntersectionType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_INTERSECTION : KaIntersectionTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaIntersectionType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                printCollection(type.conjuncts, separator = " & ") {
                    typeRenderer.renderType(analysisSession, it, printer)
                }
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaIntersectionTypeRenderer' instead", ReplaceWith("KaIntersectionTypeRenderer"))
public typealias KtIntersectionTypeRenderer = KaIntersectionTypeRenderer