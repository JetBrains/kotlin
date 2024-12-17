/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.lang.ASTNode
import com.intellij.psi.FileViewProvider
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationImpl
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtVisitor

class KtFileWithExpressions(
    viewProvider: FileViewProvider,
) : KtFile(viewProvider, isCompiled = false) {
    init {
        contentElementType = KtNodeTypes.FILE_WITH_EXPRESSIONS
    }

    val expressionHolders: List<KtFileExpressionHolder>
        get() = findChildrenByClass(KtFileExpressionHolder::class.java).toList()
}

class KtFileExpressionHolder(node: ASTNode) : KtDeclarationImpl(node) {
    val block: KtBlockExpression?
        get() = findChildByClass(KtBlockExpression::class.java)

    val expressions: List<KtExpression>
        get() = block?.statements.orEmpty()

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R? = visitor.visitKtFileExpressionHolder(this, data)
}