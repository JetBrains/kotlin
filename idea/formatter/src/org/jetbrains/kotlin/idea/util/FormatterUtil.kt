/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.formatting.ASTBlock
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.kotlinCustomSettings
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.cast

/*
 * ASTBlock.node is nullable, this extension was introduced to minimize changes
 */
fun ASTBlock.requireNode() = node ?: error("ASTBlock.getNode() returned null")

/**
 * Can be removed with all usages after moving master to 1.3 with new default code style settings.
 */
val isDefaultOfficialCodeStyle by lazy { !KotlinCodeStyleSettings.defaultSettings().CONTINUATION_INDENT_FOR_CHAINED_CALLS }

fun PsiElement.getLineCount(): Int {
    val doc = containingFile?.let { PsiDocumentManager.getInstance(project).getDocument(it) }
    if (doc != null) {
        val spaceRange = textRange ?: TextRange.EMPTY_RANGE

        if (spaceRange.endOffset <= doc.textLength && spaceRange.startOffset < spaceRange.endOffset) {
            val startLine = doc.getLineNumber(spaceRange.startOffset)
            val endLine = doc.getLineNumber(spaceRange.endOffset - 1)

            return endLine - startLine + 1
        }
    }

    return StringUtil.getLineBreakCount(text ?: "") + 1
}

fun PsiElement.isMultiline() = getLineCount() > 1

fun KtFunctionLiteral.needTrailingComma(settings: CodeStyleSettings?, checkExistingTrailingComma: Boolean = true): Boolean =
    needTrailingComma(
        settings = settings,
        trailingComma = { if (checkExistingTrailingComma) valueParameterList?.trailingComma else null },
        globalStartOffset = { valueParameterList?.startOffset },
        globalEndOffset = { arrow?.endOffset },
    )

fun KtWhenEntry.needTrailingComma(settings: CodeStyleSettings?, checkExistingTrailingComma: Boolean = true): Boolean = needTrailingComma(
    settings = settings,
    trailingComma = { if (checkExistingTrailingComma) trailingComma else null },
    additionalCheck = { !isElse && parent.cast<KtWhenExpression>().leftParenthesis != null },
    globalEndOffset = { arrow?.endOffset },
)

fun KtDestructuringDeclaration.needTrailingComma(settings: CodeStyleSettings?, checkExistingTrailingComma: Boolean = true): Boolean =
    needTrailingComma(
        settings = settings,
        trailingComma = { if (checkExistingTrailingComma) trailingComma else null },
        globalStartOffset = { lPar?.startOffset },
        globalEndOffset = { rPar?.endOffset },
    )

fun <T : PsiElement> T.needTrailingComma(
    settings: CodeStyleSettings?,
    trailingComma: T.() -> PsiElement?,
    additionalCheck: () -> Boolean = { true },
    globalStartOffset: T.() -> Int? = PsiElement::startOffset,
    globalEndOffset: T.() -> Int? = PsiElement::endOffset,
): Boolean {
    if (trailingComma() == null && settings?.kotlinCustomSettings?.addTrailingCommaIsAllowedFor(this) == false) return false
    if (!additionalCheck()) return false

    val startOffset = globalStartOffset() ?: return false
    val endOffset = globalEndOffset() ?: return false
    return containsLineBreakInThis(startOffset, endOffset)
}

fun PsiElement.containsLineBreakInThis(globalStartOffset: Int, globalEndOffset: Int): Boolean {
    val textRange = TextRange.create(globalStartOffset, globalEndOffset).shiftLeft(startOffset)
    return StringUtil.containsLineBreak(textRange.subSequence(text))
}

fun trailingCommaIsAllowedOnCallSite(): Boolean = Registry.`is`("kotlin.formatter.allowTrailingCommaOnCallSite")

private val TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE = TokenSet.create(
    KtNodeTypes.TYPE_PARAMETER_LIST,
    KtNodeTypes.DESTRUCTURING_DECLARATION,
    KtNodeTypes.WHEN_ENTRY,
    KtNodeTypes.FUNCTION_LITERAL,
    KtNodeTypes.VALUE_PARAMETER_LIST,
)

private val TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE = TokenSet.create(
    KtNodeTypes.COLLECTION_LITERAL_EXPRESSION,
    KtNodeTypes.TYPE_ARGUMENT_LIST,
    KtNodeTypes.INDICES,
    KtNodeTypes.VALUE_ARGUMENT_LIST,
)

private val TYPES_WITH_TRAILING_COMMA = TokenSet.orSet(
    TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE,
    TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE,
)

fun UserDataHolder.addTrailingCommaIsAllowedForThis(): Boolean {
    val type = elementType(this) ?: return false
    return type in TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE ||
            trailingCommaIsAllowedOnCallSite() && type in TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE
}

fun KotlinCodeStyleSettings.addTrailingCommaIsAllowedFor(element: UserDataHolder): Boolean =
    ALLOW_TRAILING_COMMA && element.addTrailingCommaIsAllowedForThis()

private fun elementType(userDataHolder: UserDataHolder): IElementType? = when (userDataHolder) {
    is ASTNode -> PsiUtilCore.getElementType(userDataHolder)
    is PsiElement -> PsiUtilCore.getElementType(userDataHolder)
    else -> null
}

fun PsiElement.canAddTrailingComma(): Boolean = canAddTrailingComma(this)

fun ASTNode.canAddTrailingComma(): Boolean = canAddTrailingComma(this)

private fun canAddTrailingComma(userDataHolder: UserDataHolder): Boolean = elementType(userDataHolder) in TYPES_WITH_TRAILING_COMMA

