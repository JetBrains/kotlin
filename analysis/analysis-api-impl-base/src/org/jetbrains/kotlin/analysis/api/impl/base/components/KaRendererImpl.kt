/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaRenderer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.types.Variance

@KaImplementationDetail
class KaRendererImpl(
    override val analysisSessionProvider: () -> KaSession
) : KaRenderer, KaSessionComponent<KaSession>() {
    override fun KaDeclarationSymbol.render(renderer: KaDeclarationRenderer): String = withValidityAssertion {
        return with(analysisSession) {
            prettyPrint { renderer.renderDeclaration(useSiteSession, this@render, this) }
        }
    }

    override fun KaType.render(renderer: KaTypeRenderer, position: Variance): String = withValidityAssertion {
        return with(analysisSession) {
            val approximatedType = KaRendererTypeApproximator.TO_DENOTABLE.approximateType(useSiteSession, this@render, position)
            prettyPrint { renderer.renderType(useSiteSession, approximatedType, this) }
        }
    }
}