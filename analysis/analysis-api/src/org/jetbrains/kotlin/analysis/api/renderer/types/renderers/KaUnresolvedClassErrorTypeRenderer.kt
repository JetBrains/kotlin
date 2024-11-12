/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaUnresolvedClassErrorTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaClassErrorType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object UNRESOLVED_QUALIFIER : KaUnresolvedClassErrorTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaClassErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, printer)
                typeRenderer.classIdRenderer.renderClassTypeQualifier(analysisSession, type, type.qualifiers, typeRenderer, printer)
            }
        }
    }

    @KaExperimentalApi
    public object AS_ERROR_WORD : KaUnresolvedClassErrorTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaClassErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR_TYPE")
        }
    }

    @KaExperimentalApi
    public object WITH_ERROR_MESSAGE : KaUnresolvedClassErrorTypeRenderer {
        @OptIn(KaNonPublicApi::class)
        override fun renderType(
            analysisSession: KaSession,
            type: KaClassErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR_TYPE(${type.errorMessage})")
        }
    }
}
