/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.utils.errors.shouldIjPlatformExceptionBeRethrown
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext

object KtFirReferenceResolver : ResolveCache.PolyVariantResolver<KtReference> {
    class KotlinResolveResult(element: PsiElement) : PsiElementResolveResult(element)

    @OptIn(KtAllowAnalysisOnEdt::class)
    override fun resolve(ref: KtReference, incompleteCode: Boolean): Array<ResolveResult> {
        check(ref is KtFirReference) { "reference should be FirKtReference, but was ${ref::class}" }
        check(ref is AbstractKtReference<*>) { "reference should be AbstractKtReference, but was ${ref::class}" }
        return allowAnalysisOnEdt {
            val resolveToPsiElements = try {
                analyze(ref.expression) { ref.getResolvedToPsi(this) }
            } catch (e: Throwable) {
                if (shouldIjPlatformExceptionBeRethrown(e)) throw e

                throw KtReferenceResolveException(ref, e)
            }
            resolveToPsiElements.map { KotlinResolveResult(it) }.toTypedArray()
        }
    }
}

class KtReferenceResolveException(
    reference: KtReference,
    cause: Throwable
) : RuntimeException("Reference is:\n${reference.element.getElementTextInContext()}", cause)