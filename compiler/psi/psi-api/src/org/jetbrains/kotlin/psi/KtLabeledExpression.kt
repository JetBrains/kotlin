/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.LocalSearchScope

/**
 * Represents a labeled expression with a label prefix.
 *
 * ### Example:
 *
 * ```kotlin
 *    outer@ for (i in 1..10) {
 *        break@outer
 *    }
 * // ^_____________________^
 * // The entire `for` block from `outer@' to '}'
 * ```
 */
class KtLabeledExpression(node: ASTNode) : KtExpressionWithLabel(node), PsiNameIdentifierOwner {
    @get:IfNotParsed
    val baseExpression: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitLabeledExpression(this, data)

    override fun getName() = getLabelName()

    override fun setName(name: String): PsiElement {
        getTargetLabel()?.replace(KtPsiFactory(project).createLabeledExpression(name).getTargetLabel()!!)
        return this
    }

    override fun getNameIdentifier() = getTargetLabel()?.getIdentifier()

    override fun getUseScope() = LocalSearchScope(this)
}
