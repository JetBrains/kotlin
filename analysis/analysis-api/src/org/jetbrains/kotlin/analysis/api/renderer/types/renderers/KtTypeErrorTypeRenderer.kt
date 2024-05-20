/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaTypeErrorType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KaTypeErrorTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaTypeErrorType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_CODE_IF_POSSIBLE : KaTypeErrorTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaTypeErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            type.tryRenderAsNonErrorType()?.let {
                printer.append(it)
                return
            }
            printer.append("ERROR")
        }
    }

    public object AS_ERROR_WORD : KaTypeErrorTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaTypeErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR")
        }
    }

    public object WITH_ERROR_MESSAGE : KaTypeErrorTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaTypeErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR(${type.errorMessage})")
        }
    }
}

public typealias KtTypeErrorTypeRenderer = KaTypeErrorTypeRenderer