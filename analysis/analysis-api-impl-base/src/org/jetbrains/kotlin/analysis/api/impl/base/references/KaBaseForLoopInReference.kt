/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.references

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.symbols
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtForLoopInReference
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor

@OptIn(KtImplementationDetail::class)
internal class KaBaseForLoopInReference(expression: KtForExpression) : KtForLoopInReference(expression), KaBaseReference {
    @OptIn(KtExperimentalApi::class)
    override fun KaSession.resolveToSymbols(): Collection<KaSymbol> {
        return element.tryResolveSymbols()?.symbols.orEmpty()
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaBaseReference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KtForExpression> {
        override val elementClass: Class<KtForExpression>
            get() = KtForExpression::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtForExpression>
            get() = { listOf(KaBaseForLoopInReference(it)) }
    }
}
