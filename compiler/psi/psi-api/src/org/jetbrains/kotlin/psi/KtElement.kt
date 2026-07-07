/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference

/**
 * The root interface of the Kotlin PSI (Program Structure Interface) hierarchy.
 *
 * A [KtElement] is a single node in the syntax tree of a Kotlin source file. Every Kotlin-specific PSI node —
 * expressions, declarations, type references, and so on — implements this interface, so it is the common entry point
 * for tree traversal and source attribution. The root of a tree of [KtElement]s is a [KtFile].
 *
 * [KtElement] describes only the *syntactic* structure of the code. To reason about its *semantics* (resolved symbols,
 * types, overload resolution, and so on), use the [Analysis API](https://kotl.in/analysis-api) on top of the PSI.
 *
 * Prefer type-safe traversal via the visitors ([accept], [acceptChildren], [KtVisitor], [KtTreeVisitor]) over the
 * generic [com.intellij.psi.PsiElement] navigation methods where possible.
 */
interface KtElement : NavigatablePsiElement, KtPureElement {
    /**
     * Passes each direct child of this element to the given [visitor].
     *
     * This does not visit the element itself, nor does it recurse into grandchildren; the visitor decides whether to
     * descend further. Use [KtTreeVisitor] for automatic recursive traversal.
     */
    fun <D> acceptChildren(visitor: KtVisitor<Void, D>, data: D)

    /**
     * Dispatches this element to the corresponding `visit*` method of the given [visitor] and returns its result.
     *
     * This is the type-safe way to handle an element based on its concrete Kotlin PSI type, as an alternative to a
     * chain of `is`/`instanceof` checks.
     */
    fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R

    /**
     * Deletes this PSI element using the raw platform implementation, bypassing Kotlin PSI-specific [delete] overrides.
     */
    @KtNonPublicApi
    fun rawDelete()

    @Deprecated("Don't use getReference() on KtElement for the choice is unpredictable")
    override fun getReference(): PsiReference?
}

/**
 * Returns the modification stamp of the nearest enclosing element that tracks one (a file, a stub-based declaration, or
 * a supertype list). The stamp changes whenever that element's subtree is modified, so it can be used to invalidate
 * caches keyed on this element.
 */
fun KtElement.getModificationStamp(): Long = when (this) {
    is PsiFile -> this.modificationStamp
    is KtDeclarationStub<*> -> this.modificationStamp
    is KtSuperTypeList -> this.modificationStamp
    else -> (parent as KtElement).getModificationStamp()
}
