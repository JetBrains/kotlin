/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache

object KtFirReferenceResolver : ResolveCache.PolyVariantResolver<KtReference> {
    class KotlinResolveResult(element: PsiElement) : PsiElementResolveResult(element)

    override fun resolve(ref: KtReference, incompleteCode: Boolean): Array<ResolveResult> {
        check(ref is AbstractKtReference<*>) { "reference should be AbstractKtReference, but was ${ref::class}" }
        val resolveToPsiElements = FirReferenceResolveHelper.resolveToPsiElements(ref)
        return resolveToPsiElements.map { KotlinResolveResult(it) }.toTypedArray()
    }
}