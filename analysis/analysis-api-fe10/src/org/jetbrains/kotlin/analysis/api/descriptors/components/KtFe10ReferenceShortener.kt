/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.analysis.api.components.KtReferenceShortener
import org.jetbrains.kotlin.analysis.api.components.ShortenCommand
import org.jetbrains.kotlin.analysis.api.components.ShortenOption
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtUserType

internal class KtFe10ReferenceShortener(
    override val analysisSession: KtFe10AnalysisSession
) : KtReferenceShortener(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun collectShortenings(
        file: KtFile,
        selection: TextRange,
        classShortenOption: (KtClassLikeSymbol) -> ShortenOption,
        callableShortenOption: (KtCallableSymbol) -> ShortenOption
    ): ShortenCommand {
        // Compiler implementation does nothing.
        // Descriptor-based shortening is implemented on the IDE plugin side.
        return object : ShortenCommand {

            // Is it better to make these properties nullable, what affects FIR KtReferenceShortener usage, or assign
            // proxy values, since FE10 implementation is not supposed to be called anyway yet?
            override val targetFile: KtFile? = null
            override val importsToAdd: List<FqName>? = null
            override val starImportsToAdd: List<FqName>? = null
            override val typesToShorten: List<SmartPsiElementPointer<KtUserType>>? = null
            override val qualifiersToShorten: List<SmartPsiElementPointer<KtDotQualifiedExpression>>? = null

            override val isEmpty: Boolean
                get() = true

            override fun invokeShortening() {}
        }
    }
}