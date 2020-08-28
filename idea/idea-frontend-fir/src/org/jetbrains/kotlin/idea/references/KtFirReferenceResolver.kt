/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.util.getElementTextInContext

object KtFirReferenceResolver : ResolveCache.PolyVariantResolver<KtReference> {
    class KotlinResolveResult(element: PsiElement) : PsiElementResolveResult(element)

    override fun resolve(ref: KtReference, incompleteCode: Boolean): Array<ResolveResult> {
        check(ref is KtFirReference) { "reference should be FirKtReference, but was ${ref::class}" }
        check(ref is AbstractKtReference<*>) { "reference should be AbstractKtReference, but was ${ref::class}" }
        if (ApplicationManager.getApplication().isDispatchThread) {
            throw ProcessCanceledException()
        }
        val resolveToPsiElements = try {
            analyze(ref.expression) { ref.getResolvedToPsi(this) }
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e
            throw KtReferenceResolveException(ref, e)
        }
        return resolveToPsiElements.map { KotlinResolveResult(it) }.toTypedArray()
    }
}

class KtReferenceResolveException(
    reference: KtReference,
    cause: Throwable
) : RuntimeException("Reference is:\n${reference.element.getElementTextInContext()}", cause)