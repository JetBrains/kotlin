/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter


public interface KtDefinitelyNotNullTypeRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderType(type: KtDefinitelyNotNullType, printer: PrettyPrinter)

    public object AS_TYPE_INTERSECTION : KtDefinitelyNotNullTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtDefinitelyNotNullType, printer: PrettyPrinter): Unit = printer {
            renderType(type.original, printer)
            printer.append(" & ")
            renderType(builtinTypes.ANY, printer)
        }
    }

}
