/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaPropertyAccessorBodyRenderer {
    public fun renderBody(analysisSession: KaSession, symbol: KaPropertyAccessorSymbol, printer: PrettyPrinter)

    public object NO_BODY : KaPropertyAccessorBodyRenderer {
        override fun renderBody(analysisSession: KaSession, symbol: KaPropertyAccessorSymbol, printer: PrettyPrinter) {}
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaPropertyAccessorBodyRenderer' instead", ReplaceWith("KaPropertyAccessorBodyRenderer"))
public typealias KtPropertyAccessorBodyRenderer = KaPropertyAccessorBodyRenderer