/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getTagIfSubject
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor

@OptIn(KtImplementationDetail::class)
internal class KaFirKDocReference(element: KDocName) : KDocReference(element), KaFirReference {
    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        val fullFqName = generateSequence(element) { it.parent as? KDocName }.last().getQualifiedNameAsFqName()
        val selectedFqName = element.getQualifiedNameAsFqName()
        val containedTagSectionIfSubject = element.getTagIfSubject()?.knownTag

        return KDocReferenceResolver.resolveKdocFqName(
            useSiteSession,
            selectedFqName,
            fullFqName,
            contextElement = element,
            containedTagSectionIfSubject
        ).toSet()
    }

    override fun getResolvedToPsi(
        analysisSession: KaSession,
        referenceTargetSymbols: Collection<KaSymbol>,
    ): Collection<PsiElement> = with(analysisSession) {
        referenceTargetSymbols.flatMap { symbol ->
            when (symbol) {
                is KaFirSyntheticJavaPropertySymbol -> listOfNotNull(symbol.javaGetterSymbol.psi, symbol.javaSetterSymbol?.psi)
                is KaFirSymbol<*> -> getPsiDeclarations(symbol)
                else -> listOfNotNull(symbol.psi)
            }
        }
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KDocName> {
        override val elementClass: Class<KDocName>
            get() = KDocName::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KDocName>
            get() = { listOf(KaFirKDocReference(it)) }
    }
}