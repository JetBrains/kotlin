/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit

fun KtExpression.elvisOrEmpty(notNullNeeded: Boolean): String {
    if (!notNullNeeded) return ""
    val binaryExpression = getStrictParentOfType<KtBinaryExpression>()
    return if (binaryExpression?.left == this && binaryExpression.operationToken == KtTokens.ELVIS) "" else "?:"
}

fun KtExpression.shouldHaveNotNullType(): Boolean {
    val type = when (val parent = parent) {
        is KtBinaryExpression -> parent.left?.let { it.getType(it.analyze()) }
        is KtProperty -> parent.typeReference?.let { it.analyze()[BindingContext.TYPE, it] }
        is KtReturnExpression -> parent.getTargetFunctionDescriptor(analyze())?.returnType
        is KtValueArgument -> {
            val call = parent.getStrictParentOfType<KtCallExpression>()?.resolveToCall()
            (call?.getArgumentMapping(parent) as? ArgumentMatch)?.valueParameter?.type
        }
        is KtBlockExpression -> {
            if (parent.statements.lastOrNull() != this) return false
            val functionLiteral = parent.parent as? KtFunctionLiteral ?: return false
            if (functionLiteral.parent !is KtLambdaExpression) return false
            functionLiteral.analyze()[BindingContext.FUNCTION, functionLiteral]?.returnType
        }
        else -> null
    } ?: return false
    return !type.isMarkedNullable && !type.isUnit() && !type.isTypeParameter()
}

fun PsiElement.moveCaretToEnd(editor: Editor?, project: Project) {
    editor?.run {
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
        val endOffset = if (text.endsWith(")")) endOffset - 1 else endOffset
        document.insertString(endOffset, " ")
        caretModel.moveToOffset(endOffset + 1)
    }
}
