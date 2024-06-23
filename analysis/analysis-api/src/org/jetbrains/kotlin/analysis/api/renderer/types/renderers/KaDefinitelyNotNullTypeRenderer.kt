/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaDefinitelyNotNullTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaDefinitelyNotNullType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_TYPE_INTERSECTION : KaDefinitelyNotNullTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaDefinitelyNotNullType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            with(analysisSession) {
                printer {
                    typeRenderer.renderType(analysisSession, type.original, printer)
                    printer.append(" & ")
                    typeRenderer.renderType(analysisSession, builtinTypes.any, printer)
                }
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaDefinitelyNotNullTypeRenderer' instead", ReplaceWith("KaDefinitelyNotNullTypeRenderer"))
public typealias KtDefinitelyNotNullTypeRenderer = KaDefinitelyNotNullTypeRenderer