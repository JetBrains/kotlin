/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.symbols
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtArrayAccessReference
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor

@OptIn(KtImplementationDetail::class)
internal class KaFirArrayAccessReference(
    expression: KtArrayAccessExpression,
) : KtArrayAccessReference(expression), KaFirReference {
    @OptIn(KtExperimentalApi::class)
    override fun KaSession.resolveToSymbols(): Collection<KaSymbol> = tryResolveSymbols()?.symbols.orEmpty()

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KtArrayAccessExpression> {
        override val elementClass: Class<KtArrayAccessExpression>
            get() = KtArrayAccessExpression::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtArrayAccessExpression>
            get() = { listOf(KaFirArrayAccessReference(it)) }
    }
}
