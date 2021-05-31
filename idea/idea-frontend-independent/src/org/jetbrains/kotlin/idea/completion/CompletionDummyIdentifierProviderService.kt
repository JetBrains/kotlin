/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.*
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import kotlin.math.max

abstract class CompletionDummyIdentifierProviderService {

    fun correctPositionForStringTemplateEntry(context: CompletionInitializationContext): Boolean {
        val offset = context.startOffset
        val psiFile = context.file
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        if (offset > 0 && tokenBefore!!.node.elementType == KtTokens.REGULAR_STRING_PART && tokenBefore.text.startsWith(".")) {
            val prev = tokenBefore.parent.prevSibling
            if (prev != null && prev is KtSimpleNameStringTemplateEntry) {
                val expression = prev.expression
                if (expression != null) {
                    val prefix = tokenBefore.text.substring(0, offset - tokenBefore.startOffset)
                    context.dummyIdentifier = "{" + expression.text + prefix + CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "}"
                    context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, expression.startOffset)
                    return true
                }
            }
        }
        return false
    }

    fun provideDummyIdentifier(context: CompletionInitializationContext): String {
        val psiFile = context.file
        if (psiFile !is KtFile) {
            error("CompletionDummyIdentifierProviderService.providerDummyIdentifier should not be called for non KtFile")
        }

        val offset = context.startOffset
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        return when {
            context.completionType == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER

            // TODO package completion

            isInClassHeader(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER // do not add '$' to not interrupt class declaration parsing

            isInUnclosedSuperQualifier(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">"

            isInSimpleStringTemplate(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED

            else -> specialLambdaSignatureDummyIdentifier(tokenBefore)
                ?: specialExtensionReceiverDummyIdentifier(tokenBefore)
                ?: specialInTypeArgsDummyIdentifier(tokenBefore)
                ?: specialInArgumentListDummyIdentifier(tokenBefore)
                ?: isInTypeParametersList(tokenBefore)
                ?: handleDefaultCase(context)
                ?: DEFAULT_DUMMY_IDENTIFIER
        }
    }

    protected open fun handleDefaultCase(context: CompletionInitializationContext): String? = null

    private fun isInTypeParametersList(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null
        if (tokenBefore.parents.any { it is KtTypeParameterList }) {
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
        }
        return null
    }

    private fun specialLambdaSignatureDummyIdentifier(tokenBefore: PsiElement?): String? {
        var leaf = tokenBefore
        while (leaf is PsiWhiteSpace || leaf is PsiComment) {
            leaf = leaf.prevLeaf(true)
        }

        val lambda = leaf?.parents?.firstOrNull { it is KtFunctionLiteral } ?: return null

        val lambdaChild = leaf.parents.takeWhile { it != lambda }.lastOrNull()

        return if (lambdaChild is KtParameterList)
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
        else
            null

    }

    private fun isInClassHeader(tokenBefore: PsiElement?): Boolean {
        val classOrObject = tokenBefore?.parents?.firstIsInstanceOrNull<KtClassOrObject>() ?: return false
        val name = classOrObject.nameIdentifier ?: return false
        val headerEnd = classOrObject.body?.startOffset ?: classOrObject.endOffset
        val offset = tokenBefore.startOffset
        return name.endOffset <= offset && offset <= headerEnd
    }

    private fun isInUnclosedSuperQualifier(tokenBefore: PsiElement?): Boolean {
        if (tokenBefore == null) return false
        val tokensToSkip = TokenSet.orSet(TokenSet.create(KtTokens.IDENTIFIER, KtTokens.DOT), KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET)
        val tokens = generateSequence(tokenBefore) { it.prevLeaf() }
        val ltToken = tokens.firstOrNull { it.node.elementType !in tokensToSkip } ?: return false
        if (ltToken.node.elementType != KtTokens.LT) return false
        val superToken = ltToken.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment }
        return superToken?.node?.elementType == KtTokens.SUPER_KEYWORD
    }

    private fun isInSimpleStringTemplate(tokenBefore: PsiElement?): Boolean {
        return tokenBefore?.parents?.firstIsInstanceOrNull<KtStringTemplateExpression>()?.isPlain() ?: false
    }


    private fun specialExtensionReceiverDummyIdentifier(tokenBefore: PsiElement?): String? {
        var token = tokenBefore ?: return null
        var ltCount = 0
        var gtCount = 0
        val builder = StringBuilder()
        while (true) {
            val tokenType = token.node!!.elementType
            if (tokenType in declarationKeywords) {
                val balance = ltCount - gtCount
                if (balance < 0) return null
                builder.append(token.text!!.reversed())
                builder.reverse()

                var tail = "X" + ">".repeat(balance) + ".f"
                if (tokenType == KtTokens.FUN_KEYWORD) {
                    tail += "()"
                }
                builder.append(tail)

                val text = builder.toString()
                val file = KtPsiFactory(tokenBefore.project).createFile(text)
                val declaration = file.declarations.singleOrNull() ?: return null
                if (declaration.textLength != text.length) return null
                val containsErrorElement = !PsiTreeUtil.processElements(file) { it !is PsiErrorElement }
                return if (containsErrorElement) null else "$tail$"
            }
            if (tokenType !in declarationTokens) return null
            if (tokenType == KtTokens.LT) ltCount++
            if (tokenType == KtTokens.GT) gtCount++
            builder.append(token.text!!.reversed())
            token = PsiTreeUtil.prevLeaf(token) ?: return null
        }
    }

    private fun specialInTypeArgsDummyIdentifier(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null

        if (tokenBefore.getParentOfType<KtTypeArgumentList>(true) != null) { // already parsed inside type argument list
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED // do not insert '$' to not break type argument list parsing
        }

        val pair = unclosedTypeArgListNameAndBalance(tokenBefore) ?: return null
        val (nameToken, balance) = pair
        assert(balance > 0)

        val nameRef = nameToken.parent as? KtNameReferenceExpression ?: return null
        return if (allTargetsAreFunctionsOrClasses(nameRef)) {
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">".repeat(balance) + "$"
        } else {
            null
        }
    }

    protected abstract fun allTargetsAreFunctionsOrClasses(nameReferenceExpression: KtNameReferenceExpression): Boolean

    private fun unclosedTypeArgListNameAndBalance(tokenBefore: PsiElement): Pair<PsiElement, Int>? {
        val nameToken = findCallNameTokenIfInTypeArgs(tokenBefore) ?: return null
        val pair = unclosedTypeArgListNameAndBalance(nameToken)
        return if (pair == null) {
            Pair(nameToken, 1)
        } else {
            Pair(pair.first, pair.second + 1)
        }
    }

    private val callTypeArgsTokens = TokenSet.orSet(
        TokenSet.create(
            KtTokens.IDENTIFIER, KtTokens.LT, KtTokens.GT,
            KtTokens.COMMA, KtTokens.DOT, KtTokens.QUEST, KtTokens.COLON,
            KtTokens.LPAR, KtTokens.RPAR, KtTokens.ARROW
        ),
        KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
    )

    // if the leaf could be located inside type argument list of a call (if parsed properly)
    // then it returns the call name reference this type argument list would belong to
    private fun findCallNameTokenIfInTypeArgs(leaf: PsiElement): PsiElement? {
        var current = leaf
        while (true) {
            val tokenType = current.node!!.elementType
            if (tokenType !in callTypeArgsTokens) return null

            if (tokenType == KtTokens.LT) {
                val nameToken = current.prevLeaf(skipEmptyElements = true) ?: return null
                if (nameToken.node!!.elementType != KtTokens.IDENTIFIER) return null
                return nameToken
            }

            if (tokenType == KtTokens.GT) { // pass nested type argument list
                val prev = current.prevLeaf(skipEmptyElements = true) ?: return null
                val typeRef = findCallNameTokenIfInTypeArgs(prev) ?: return null
                current = typeRef
                continue
            }

            current = current.prevLeaf(skipEmptyElements = true) ?: return null
        }
    }


    private fun specialInArgumentListDummyIdentifier(tokenBefore: PsiElement?): String? {
        // If we insert $ in the argument list of a delegation specifier, this will break parsing
        // and the following block will not be attached as a body to the constructor. Therefore
        // we need to use a regular identifier.
        val argumentList = tokenBefore?.getNonStrictParentOfType<KtValueArgumentList>() ?: return null
        if (argumentList.parent is KtConstructorDelegationCall) return CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
        return null
    }

    companion object {
        val DEFAULT_DUMMY_IDENTIFIER: String =
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$" // add '$' to ignore context after the caret

        private val declarationKeywords = TokenSet.create(KtTokens.FUN_KEYWORD, KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD)
        private val declarationTokens = TokenSet.orSet(
            TokenSet.create(
                KtTokens.IDENTIFIER, KtTokens.LT, KtTokens.GT,
                KtTokens.COMMA, KtTokens.DOT, KtTokens.QUEST, KtTokens.COLON,
                KtTokens.IN_KEYWORD, KtTokens.OUT_KEYWORD,
                KtTokens.LPAR, KtTokens.RPAR, KtTokens.ARROW,
                TokenType.ERROR_ELEMENT
            ),
            KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
        )
    }
}
