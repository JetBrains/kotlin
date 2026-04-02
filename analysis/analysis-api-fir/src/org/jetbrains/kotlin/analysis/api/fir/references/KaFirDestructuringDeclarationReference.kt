/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.resolution.symbols
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

@OptIn(KtImplementationDetail::class)
internal class KaFirDestructuringDeclarationReference(
    element: KtDestructuringDeclarationEntry,
) : KtDestructuringDeclarationReference(element), KaFirReference {
    override fun canRename(): Boolean = false //todo

    @OptIn(KtExperimentalApi::class)
    override fun KaSession.resolveToSymbols(): Collection<KaSymbol> {
        val element = element
        // TODO(KT-82708): Only the initializer symbol is expected
        return listOf(element.symbol) + tryResolveSymbols()?.symbols.orEmpty()
    }

    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        shouldNotBeCalled("Only resolveToSymbols is supposed to be used directly")
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
