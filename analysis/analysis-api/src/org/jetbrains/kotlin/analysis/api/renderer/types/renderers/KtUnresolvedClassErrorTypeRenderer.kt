/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtClassErrorType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtUnresolvedClassErrorTypeRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderType(type: KtClassErrorType, printer: PrettyPrinter)

    public object UNRESOLVED_QUALIFIER : KtUnresolvedClassErrorTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtClassErrorType, printer: PrettyPrinter): Unit = printer {
            annotationsRenderer.renderAnnotations(type, printer)
            classIdRenderer.renderClassTypeQualifier(type, printer)
        }
    }


    public object AS_ERROR_WORD : KtUnresolvedClassErrorTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtClassErrorType, printer: PrettyPrinter) {
            printer.append("ERROR_TYPE")
        }
    }

    public object WITH_ERROR_MESSAGE : KtUnresolvedClassErrorTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtClassErrorType, printer: PrettyPrinter) {
            printer.append("ERROR_TYPE(${type.errorMessage})")
        }
    }
}

