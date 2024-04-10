/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtTypeErrorType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtTypeErrorTypeRenderer {
    public fun renderType(
        analysisSession: KtAnalysisSession,
        type: KtTypeErrorType,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_CODE_IF_POSSIBLE : KtTypeErrorTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtTypeErrorType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            type.tryRenderAsNonErrorType()?.let {
                printer.append(it)
                return
            }
            printer.append("ERROR")
        }
    }

    public object AS_ERROR_WORD : KtTypeErrorTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtTypeErrorType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR")
        }
    }

    public object WITH_ERROR_MESSAGE : KtTypeErrorTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtTypeErrorType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR(${type.errorMessage})")
        }
    }
}

