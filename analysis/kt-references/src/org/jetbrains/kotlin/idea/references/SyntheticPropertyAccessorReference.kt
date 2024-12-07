/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.resolve.references.ReferenceAccess
import org.jetbrains.kotlin.asJava.canHaveSyntheticSetter
import org.jetbrains.kotlin.asJava.canHaveSyntheticGetter

abstract class SyntheticPropertyAccessorReference(
    expression: KtNameReferenceExpression,
    val getter: Boolean
) : KtSimpleReference<KtNameReferenceExpression>(expression) {
    protected fun isAccessorName(name: String): Boolean {
        if (getter) {
            return name.startsWith("get") || name.startsWith("is")
        }
        return name.startsWith("set")
    }

    override fun canBeReferenceTo(candidateTarget: PsiElement): Boolean {
        if (candidateTarget !is PsiMethod || !isAccessorName(candidateTarget.name)) return false
        if (getter && !candidateTarget.canHaveSyntheticGetter || !getter && !candidateTarget.canHaveSyntheticSetter) return false
        if (!getter && expression.readWriteAccess(true) == ReferenceAccess.READ) return false
        return true
    }

    override fun getRangeInElement() = TextRange(0, expression.textLength)

    override fun canRename() = true

    override val resolvesByNames: Collection<Name>
        get() = listOf(element.getReferencedNameAsName())
}
