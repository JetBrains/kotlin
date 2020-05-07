/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReferenceExpression

interface KtReference : PsiPolyVariantReference {
    val resolver: ResolveCache.PolyVariantResolver<KtReference>

    override fun getElement(): KtElement

    val resolvesByNames: Collection<Name>
}

abstract class AbstractKtReference<T : KtElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), KtReference {
    val expression: T
        get() = element

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        ResolveCache.getInstance(expression.project).resolveWithCaching(this, resolver, false, incompleteCode)

    override fun getCanonicalText(): String = "<TBD>"

    open fun canRename(): Boolean = false
    override fun handleElementRename(newElementName: String): PsiElement? = throw IncorrectOperationException()

    override fun bindToElement(element: PsiElement): PsiElement = throw IncorrectOperationException()

    @Suppress("UNCHECKED_CAST")
    override fun getVariants(): Array<Any> = PsiReference.EMPTY_ARRAY as Array<Any>

    override fun isSoft(): Boolean = false

    override fun toString() = this::class.java.simpleName + ": " + expression.text
}

abstract class KtSimpleReference<T : KtReferenceExpression>(expression: T) : AbstractKtReference<T>(expression)

abstract class KtMultiReference<T : KtElement>(expression: T) : AbstractKtReference<T>(expression)