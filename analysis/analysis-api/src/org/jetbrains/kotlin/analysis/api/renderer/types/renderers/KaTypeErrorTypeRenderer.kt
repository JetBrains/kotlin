/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaErrorTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaErrorType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_CODE_IF_POSSIBLE : KaErrorTypeRenderer {
        @OptIn(KaAnalysisNonPublicApi::class)
        override fun renderType(
            analysisSession: KaSession,
            type: KaErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            type.presentableText?.let {
                printer.append(it)
                return
            }
            printer.append("ERROR")
        }
    }

    public object AS_ERROR_WORD : KaErrorTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR")
        }
    }

    public object WITH_ERROR_MESSAGE : KaErrorTypeRenderer {
        @OptIn(KaAnalysisNonPublicApi::class)
        override fun renderType(
            analysisSession: KaSession,
            type: KaErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR(${type.errorMessage})")
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaErrorTypeRenderer' instead", ReplaceWith("KaErrorTypeRenderer"))
public typealias KtTypeErrorTypeRenderer = KaErrorTypeRenderer