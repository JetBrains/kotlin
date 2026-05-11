/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.references

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor

@OptIn(KtImplementationDetail::class)
internal class KaBaseInvokeFunctionReference(expression: KtCallExpression) : KtInvokeFunctionReference(expression), KaBaseReference {
    @OptIn(KtExperimentalApi::class)
    override fun KaSession.resolveToSymbols(): Collection<KaSymbol> = when (val callResult = element.tryResolveCall()) {
        // There is no way to distinguish between the error regular and implicit calls, so by default only relevant errors are shown
        is KaCallResolutionError -> callResult.candidateCalls.filterIsInstance<KaImplicitInvokeCall>().map { it.symbol }
        is KaCallResolutionSuccess -> when (val call = callResult.call) {
            is KaImplicitInvokeCall -> listOf(call.symbol)
            else -> emptyList()
        }

        // Multi-call resolution attempts are never implicit invoke calls
        is KaMultiCallResolutionAttempt -> emptyList()
        null -> emptyList()
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaBaseReference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KtCallExpression> {
        override val elementClass: Class<KtCallExpression>
            get() = KtCallExpression::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtCallExpression>
            get() = { listOf(KaBaseInvokeFunctionReference(it)) }
    }
}
