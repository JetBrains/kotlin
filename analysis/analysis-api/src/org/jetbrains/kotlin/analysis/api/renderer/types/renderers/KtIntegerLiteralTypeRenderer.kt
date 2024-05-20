/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaIntegerLiteralType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KaIntegerLiteralTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaIntegerLiteralType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_ILT_WITH_VALUE : KaIntegerLiteralTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaIntegerLiteralType,
            typeRenderer: KaTypeRenderer,
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

public typealias KtIntegerLiteralTypeRenderer = KaIntegerLiteralTypeRenderer