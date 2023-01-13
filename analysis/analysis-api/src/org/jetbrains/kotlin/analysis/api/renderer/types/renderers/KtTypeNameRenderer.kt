/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.render

public interface KtTypeNameRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderName(name: Name, owner: KtType, printer: PrettyPrinter)

    public object QUOTED : KtTypeNameRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderName(name: Name, owner: KtType, printer: PrettyPrinter) {
            printer.append(name.render())
        }
    }

    public object UNQUOTED : KtTypeNameRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderName(name: Name, owner: KtType, printer: PrettyPrinter) {
            printer.append(name.asString())
        }
    }

}
