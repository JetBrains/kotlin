/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.components.KtSymbolDeclarationRendererProvider
import org.jetbrains.kotlin.analysis.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.render
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KtFe10TypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType

internal class KtFe10SymbolDeclarationRendererProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtSymbolDeclarationRendererProvider(), Fe10KtAnalysisSessionComponent {
    override val token: ValidityToken
        get() = analysisSession.token

    override fun render(symbol: KtSymbol, options: KtDeclarationRendererOptions): String {
        val descriptor = getSymbolDescriptor(symbol)
        if (descriptor != null) {
            return descriptor.render(analysisContext, options)
        }

        // Rendering for unresolved symbols is not implemented
        return ""
    }

    override fun render(type: KtType, options: KtTypeRendererOptions): String {
        require(type is KtFe10Type)
        val consumer = StringBuilder()
        KtFe10TypeRenderer(options).render(type.type, consumer)
        return consumer.toString()
    }
}