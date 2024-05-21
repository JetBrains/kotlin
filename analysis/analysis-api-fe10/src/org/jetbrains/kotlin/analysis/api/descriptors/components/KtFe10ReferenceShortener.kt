/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

internal class KaFe10ReferenceShortener(
    override val analysisSession: KaFe10Session,
) : KaReferenceShortener(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun collectShortenings(
        file: KtFile,
        selection: TextRange,
        shortenOptions: ShortenOptions,
        classShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy,
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