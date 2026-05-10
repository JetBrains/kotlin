/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.symbols
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor

@OptIn(KtImplementationDetail::class)
internal class KaFirDestructuringDeclarationReference(
    element: KtDestructuringDeclarationEntry,
) : KtDestructuringDeclarationReference(element), KaFirReference {
    override fun getRangeInElement(): TextRange {
        val valOrVarKeyword = element.ownValOrVarKeyword
        if (valOrVarKeyword != null) {
            return element.initializer?.textRangeInParent ?: element.nameIdentifier?.textRangeInParent ?: super.getRangeInElement()
        }
        return super.getRangeInElement()
    }

    override fun canRename(): Boolean {
        return element.ownValOrVarKeyword != null &&
                (element.parent as? KtDestructuringDeclaration)?.hasSquareBrackets() != true
    }

    @OptIn(KtExperimentalApi::class)
    override fun KaSession.resolveToSymbols(): Collection<KaSymbol> {
        val element = element
        // TODO(KT-82708): Only the initializer symbol is expected
        return listOf(element.symbol) + tryResolveSymbols()?.symbols.orEmpty()
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KtDestructuringDeclarationEntry> {
        override val elementClass: Class<KtDestructuringDeclarationEntry>
            get() = KtDestructuringDeclarationEntry::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtDestructuringDeclarationEntry>
            get() = { listOf(KaFirDestructuringDeclarationReference(it)) }
    }
}
