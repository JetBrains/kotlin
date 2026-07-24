/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaRenderer
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.analysis.api.renderer.render as renderEndpoint

@KaImplementationDetail
internal class KaRendererBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaRenderer {
    override fun KaDeclarationSymbol.render(renderer: KaDeclarationRenderer): String =
        context(analysisSession) { renderEndpoint(renderer) }

    override fun KaType.render(renderer: KaTypeRenderer, position: Variance): String =
        context(analysisSession) { renderEndpoint(renderer, position) }
}
