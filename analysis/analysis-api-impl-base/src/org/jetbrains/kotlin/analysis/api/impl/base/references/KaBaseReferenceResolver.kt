/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.references

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.permissions.*
import org.jetbrains.kotlin.idea.references.AbstractKtReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.utils.exceptions.buildErrorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal object KaBaseReferenceResolver : ResolveCache.PolyVariantResolver<KtReference> {
    private val LOG = Logger.getInstance(KaBaseReferenceResolver::class.java)

    class KotlinResolveResult(element: PsiElement) : PsiElementResolveResult(element)

    @OptIn(KaAllowAnalysisOnEdt::class, KaAllowAnalysisFromWriteAction::class)
    override fun resolve(ref: KtReference, incompleteCode: Boolean): Array<ResolveResult> {
        check(ref is KaBaseReference) { "reference should be FirKtReference, but was ${ref::class}" }
        check(ref is AbstractKtReference<*>) { "reference should be AbstractKtReference, but was ${ref::class}" }
        return allowAnalysisOnEdt {
            @Suppress("INVISIBLE_REFERENCE")
            @OptIn(KaAllowProhibitedAnalyzeFromWriteAction::class)
            allowAnalysisFromWriteAction {
                val resolveToPsiElements = try {
                    analyze(ref.expression) { ref.getResolvedToPsi(this) }
                } catch (exception: Exception) {
                    rethrowIntellijPlatformExceptionIfNeeded(exception)

                    val wrappedException = buildErrorWithAttachment("Unable to resolve reference ${ref.element::class}", exception) {
                        withPsiEntry("reference", ref.element)
                    }

                    LOG.error(wrappedException)

                    emptyList()
                }

                resolveToPsiElements.map { KotlinResolveResult(it) }.toTypedArray()
            }
        }
    }
}
