/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor

@OptIn(KtImplementationDetail::class)
internal class KaFirDefaultAnnotationArgumentReference(
    element: KtValueArgument,
) : KtDefaultAnnotationArgumentReference(element), KaFirReference {
    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        val annotationEntry = element.getStrictParentOfType<KtAnnotationEntry>() ?: return emptyList()
        val constructorSymbol = annotationEntry.resolveSymbol() ?: return emptyList()
        val firstParam = constructorSymbol.valueParameters.firstOrNull() ?: return emptyList()
        return listOf(firstParam)
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KtValueArgument> {
        override val elementClass: Class<KtValueArgument>
            get() = KtValueArgument::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtValueArgument>
            get() = { element ->
                if (element.shouldProduceReference()) {
                    listOf(KaFirDefaultAnnotationArgumentReference(element))
                } else {
                    emptyList()
                }
            }
    }
}
