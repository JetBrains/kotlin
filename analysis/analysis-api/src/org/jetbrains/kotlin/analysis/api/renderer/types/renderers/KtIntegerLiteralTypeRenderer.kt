/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtIntegerLiteralType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter


public interface KtIntegerLiteralTypeRenderer {
    public fun renderType(
        analysisSession: KtAnalysisSession,
        type: KtIntegerLiteralType,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_ILT_WITH_VALUE : KtIntegerLiteralTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtIntegerLiteralType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                append("ILT(")
                printer.append(type.value.toString())
                append(')')
            }
        }
    }
}
