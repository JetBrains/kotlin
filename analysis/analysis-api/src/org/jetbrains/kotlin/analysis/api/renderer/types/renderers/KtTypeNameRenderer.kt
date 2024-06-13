/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.render

public interface KaTypeNameRenderer {
    public fun renderName(
        analysisSession: KaSession,
        name: Name,
        owner: KaType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object QUOTED : KaTypeNameRenderer {
        override fun renderName(
            analysisSession: KaSession,
            name: Name,
            owner: KaType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append(name.render())
        }
    }

    public object UNQUOTED : KaTypeNameRenderer {
        override fun renderName(
            analysisSession: KaSession,
            name: Name,
            owner: KaType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append(name.asString())
        }
    }
}

public typealias KtTypeNameRenderer = KaTypeNameRenderer