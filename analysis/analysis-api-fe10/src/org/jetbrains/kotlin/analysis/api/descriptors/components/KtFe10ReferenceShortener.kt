/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtThisExpression

internal class KtFe10ReferenceShortener(
    override val analysisSession: KtFe10AnalysisSession,
) : KtReferenceShortener(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun collectShortenings(
        file: KtFile,
        selection: TextRange,
        shortenOptions: ShortenOptions,
        classShortenStrategy: (KtClassLikeSymbol) -> ShortenStrategy,
        callableShortenStrategy: (KtCallableSymbol) -> ShortenStrategy,
    ): ShortenCommand {
        // Compiler implementation does nothing.
        // Descriptor-based shortening is implemented on the IDE plugin side.
        val ktFilePointer = SmartPointerManager.createPointer(file)

        return object : ShortenCommand {
            override val targetFile: SmartPsiElementPointer<KtFile> get() = ktFilePointer
            override val importsToAdd: Set<FqName> get() = emptySet()
            override val starImportsToAdd: Set<FqName> get() = emptySet()
            override val listOfTypeToShortenInfo: List<TypeToShortenInfo> get() = emptyList()
            override val listOfQualifierToShortenInfo: List<QualifierToShortenInfo> get() = emptyList()
            override val thisLabelsToShorten: List<ThisLabelToShortenInfo> = emptyList()
            override val kDocQualifiersToShorten: List<SmartPsiElementPointer<KDocName>> get() = emptyList()

            override val isEmpty: Boolean get() = true
        }
    }
}