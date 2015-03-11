/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns.elementType
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public class KotlinCompletionContributor : CompletionContributor() {

    private val AFTER_NUMBER_LITERAL = psiElement().afterLeafSkipping(psiElement().withText(""), psiElement().withElementType(elementType().oneOf(JetTokens.FLOAT_LITERAL, JetTokens.INTEGER_LITERAL)))
    private val AFTER_INTEGER_LITERAL_AND_DOT = psiElement().afterLeafSkipping(psiElement().withText("."), psiElement().withElementType(elementType().oneOf(JetTokens.INTEGER_LITERAL)))

    private val DEFAULT_DUMMY_IDENTIFIER = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$" // add '$' to ignore context after the caret

    ;{
        val provider = object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                performCompletion(parameters, result)
            }
        }
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), provider)
        extend(CompletionType.SMART, PlatformPatterns.psiElement(), provider)
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val psiFile = context.getFile()
        if (psiFile !is JetFile) return

        val offset = context.getStartOffset()
        val tokenBefore = psiFile.findElementAt(Math.max(0, offset - 1))

        val dummyIdentifier = when {
            context.getCompletionType() == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER

            PackageDirectiveCompletion.ACTIVATION_PATTERN.accepts(tokenBefore) -> PackageDirectiveCompletion.DUMMY_IDENTIFIER

            isInFunctionLiteralParameterList(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED

            isInClassHeader(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER // do not add '$' to not interrupt class declaration parsing

            else -> specialExtensionReceiverDummyIdentifier(tokenBefore)
                    ?: specialInTypeArgsDummyIdentifier(tokenBefore)
                    ?: specialInParameterListDummyIdentifier(tokenBefore)
                    ?: DEFAULT_DUMMY_IDENTIFIER
        }
        context.setDummyIdentifier(dummyIdentifier)

        // this code will make replacement offset "modified" and prevents altering it by the code in CompletionProgressIndicator
        context.setReplacementOffset(context.getReplacementOffset())

        if (context.getCompletionType() == CompletionType.SMART && !isAtEndOfLine(offset, context.getEditor().getDocument()) /* do not use parent expression if we are at the end of line - it's probably parsed incorrectly */) {
            val tokenAt = psiFile.findElementAt(Math.max(0, offset))
            if (tokenAt != null) {
                var parent = tokenAt.getParent()
                if (parent is JetExpression && parent !is JetBlockExpression) {
                    // search expression to be replaced - go up while we are the first child of parent expression
                    var expression = parent as JetExpression
                    parent = expression.getParent()
                    while (parent is JetExpression && parent!!.getFirstChild() == expression) {
                        expression = parent as JetExpression
                        parent = expression.getParent()
                    }

                    val expressionEnd = expression.getTextRange()!!.getEndOffset()
                    val suggestedReplacementOffset = if (expression is JetCallExpression) {
                        val calleeExpression = (expression as JetCallExpression).getCalleeExpression()
                        if (calleeExpression != null) calleeExpression.getTextRange()!!.getEndOffset() else expressionEnd
                    }
                    else {
                        expressionEnd
                    }
                    if (suggestedReplacementOffset > context.getReplacementOffset()) {
                        context.setReplacementOffset(suggestedReplacementOffset)
                    }

                    context.getOffsetMap().addOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET, expressionEnd)

                    val argumentList = (expression.getParent() as? JetValueArgument)?.getParent() as? JetValueArgumentList
                    if (argumentList != null) {
                        context.getOffsetMap().addOffset(SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET,
                                                         argumentList.getRightParenthesis()?.getTextRange()?.getStartOffset() ?: argumentList.getTextRange().getEndOffset())
                    }
                }
            }
        }
    }

    private fun isInFunctionLiteralParameterList(tokenBefore: PsiElement?): Boolean {
        val parameterList = tokenBefore?.parents(false)?.firstOrNull { it is JetParameterList } ?: return false
        val parent = parameterList.getParent()
        return parent is JetFunctionLiteral && parent.getValueParameterList() == parameterList
    }

    private fun isInClassHeader(tokenBefore: PsiElement?): Boolean {
        val classOrObject = tokenBefore?.parents(false)?.firstIsInstanceOrNull<JetClassOrObject>() ?: return false
        val name = classOrObject.getNameIdentifier() ?: return false
        val body = classOrObject.getBody() ?: return false
        val offset = tokenBefore!!.getTextRange().getStartOffset()
        return name.getTextRange().getEndOffset() <= offset && offset <= body.getTextRange().getStartOffset()
    }

    private val declarationKeywords = TokenSet.create(JetTokens.FUN_KEYWORD, JetTokens.VAL_KEYWORD, JetTokens.VAR_KEYWORD)
    private val declarationTokens = TokenSet.orSet(TokenSet.create(JetTokens.IDENTIFIER, JetTokens.LT, JetTokens.GT,
                                                                   JetTokens.COMMA, JetTokens.DOT, JetTokens.QUEST, JetTokens.COLON,
                                                                   JetTokens.IN_KEYWORD, JetTokens.OUT_KEYWORD,
                                                                   JetTokens.LPAR, JetTokens.RPAR, JetTokens.ARROW,
                                                                   TokenType.ERROR_ELEMENT),
                                                   JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET)

    private fun specialExtensionReceiverDummyIdentifier(tokenBefore: PsiElement?): String? {
        var token = tokenBefore ?: return null
        var ltCount = 0
        var gtCount = 0
        val builder = StringBuilder()
        while (true) {
            val tokenType = token.getNode()!!.getElementType()
            if (tokenType in declarationKeywords) {
                val balance = ltCount - gtCount
                if (balance < 0) return null
                builder.append(token.getText()!!.reverse())
                builder.reverse()

                var tail = "X" + ">".repeat(balance) + ".f"
                if (tokenType == JetTokens.FUN_KEYWORD) {
                    tail += "()"
                }
                builder append tail

                val text = builder.toString()
                val file = JetPsiFactory(tokenBefore.getProject()).createFile(text)
                val declaration = file.getDeclarations().singleOrNull() ?: return null
                if (declaration.getTextLength() != text.length) return null
                val containsErrorElement = !PsiTreeUtil.processElements(file, PsiElementProcessor<PsiElement>{ it !is PsiErrorElement })
                return if (containsErrorElement) null else tail + "$"
            }
            if (tokenType !in declarationTokens) return null
            if (tokenType == JetTokens.LT) ltCount++
            if (tokenType == JetTokens.GT) gtCount++
            builder.append(token.getText()!!.reverse())
            token = PsiTreeUtil.prevLeaf(token) ?: return null
        }
    }

    private fun performCompletion(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.getPosition()
        if (position.getContainingFile() !is JetFile) return

        if (position.getNonStrictParentOfType<PsiComment>() != null) {
            // don't stop here, allow other contributors to run
            return
        }

        if (shouldSuppressCompletion(parameters, result.getPrefixMatcher())) {
            result.stopHere()
            return
        }

        if (PackageDirectiveCompletion.perform(parameters, result)) {
            result.stopHere()
            return
        }

        try {
            result.restartCompletionWhenNothingMatches()

            val configuration = CompletionSessionConfiguration(parameters)
            if (parameters.getCompletionType() == CompletionType.BASIC) {
                val session = BasicCompletionSession(configuration, parameters, result)

                if (session.completionKind == BasicCompletionSession.CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME && parameters.isAutoPopup()) {
                    result.stopHere()
                    return
                }

                val somethingAdded = session.complete()
                if (!somethingAdded && parameters.getInvocationCount() < 2) {
                    // Rerun completion if nothing was found
                    val newConfiguration = CompletionSessionConfiguration(completeNonImportedDeclarations = true,
                                                                          completeNonAccessibleDeclarations = false)
                    BasicCompletionSession(newConfiguration, parameters, result).complete()
                }
            }
            else {
                SmartCompletionSession(configuration, parameters, result).complete()
            }
        }
        catch (e: ProcessCanceledException) {
            throw rethrowWithCancelIndicator(e)
        }
    }

    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.getPosition()
        val invocationCount = parameters.getInvocationCount()

        // no completion inside number literals
        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // no completion auto-popup after integer and dot
        if (invocationCount == 0 && prefixMatcher.getPrefix().isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        // no auto-popup on typing after "val", "var" and "fun" because it's likely the name of the declaration which is being typed by user
        if (invocationCount == 0 && isInExtensionReceiver(position)) return true

        return false
    }

    private fun isInExtensionReceiver(position: PsiElement): Boolean {
        val nameRef = position.getParent() as? JetNameReferenceExpression ?: return false
        val userType = nameRef.getParent() as? JetUserType ?: return false
        val typeRef = userType.getParent() as? JetTypeReference ?: return false
        if (userType != typeRef.getTypeElement()) return false
        val parent = typeRef.getParent()
        return when (parent) {
            is JetNamedFunction -> typeRef == parent.getReceiverTypeReference()
            is JetProperty -> typeRef == parent.getReceiverTypeReference()
            else -> false
        }
    }

    private fun isAtEndOfLine(offset: Int, document: Document): Boolean {
        var i = offset
        val chars = document.getCharsSequence()
        while (i < chars.length()) {
            val c = chars.charAt(i)
            if (c == '\n') return true
            if (!Character.isWhitespace(c)) return false
            i++
        }
        return true
    }

    private fun specialInTypeArgsDummyIdentifier(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null
        val pair = unclosedTypeArgListNameAndBalance(tokenBefore) ?: return null
        val (nameToken, balance) = pair
        assert(balance > 0)

        val nameRef = nameToken.getParent() as? JetNameReferenceExpression ?: return null
        val bindingContext = nameRef.getResolutionFacade().analyze(nameRef, BodyResolveMode.PARTIAL)
        val target = bindingContext[BindingContext.REFERENCE_TARGET, nameRef]
        val targets = if (target != null) {
            listOf(target)
        }
        else {
            bindingContext[BindingContext.AMBIGUOUS_REFERENCE_TARGET, nameRef] ?: return null
        }
        if (targets.all { it is FunctionDescriptor || it is ClassDescriptor && it.getKind() == ClassKind.CLASS }) {
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">".repeat(balance) + "$"
        }
        else {
            return null
        }
    }

    private fun unclosedTypeArgListNameAndBalance(tokenBefore: PsiElement): Pair<PsiElement, Int>? {
        if (tokenBefore.getParentOfType<JetTypeArgumentList>(true) != null) return null // already parsed inside type argument list
        val nameToken = findCallNameTokenIfInTypeArgs(tokenBefore) ?: return null
        val pair = unclosedTypeArgListNameAndBalance(nameToken)
        if (pair == null) {
            return Pair(nameToken, 1)
        }
        else {
            return Pair(pair.first, pair.second + 1)
        }
    }

    private val callTypeArgsTokens = TokenSet.orSet(TokenSet.create(JetTokens.IDENTIFIER, JetTokens.LT, JetTokens.GT,
                                                                   JetTokens.COMMA, JetTokens.DOT, JetTokens.QUEST, JetTokens.COLON,
                                                                   JetTokens.LPAR, JetTokens.RPAR, JetTokens.ARROW),
                                                   JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET)

    // if the leaf could be located inside type argument list of a call (if parsed properly)
    // then it returns the call name reference this type argument list would belong to
    private fun findCallNameTokenIfInTypeArgs(leaf: PsiElement): PsiElement? {
        var current = leaf
        while (true) {
            val tokenType = current.getNode()!!.getElementType()
            if (tokenType !in callTypeArgsTokens) return null

            if (tokenType == JetTokens.LT) {
                val nameToken = current.prevLeaf(skipEmptyElements = true) ?: return null
                if (nameToken.getNode()!!.getElementType() != JetTokens.IDENTIFIER) return null
                return nameToken
            }

            if (tokenType == JetTokens.GT) { // pass nested type argument list
                val prev = current.prevLeaf(skipEmptyElements = true) ?: return null
                val typeRef = findCallNameTokenIfInTypeArgs(prev) ?: return null
                current = typeRef
                continue
            }

            current = current.prevLeaf(skipEmptyElements = true) ?: return null
        }
    }

    private fun specialInParameterListDummyIdentifier(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null
        var parent = tokenBefore.getParent()
        while (parent != null) {
            if (parent is JetParameterList) {
                val balance = countParenthesisBalance(tokenBefore, parent)
                val count = if (balance > 1) balance - 1 else 0
                return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ")".repeat(count) + " a: B$"
            }
            if (parent is JetTypeElement) return null
            if (parent is JetAnnotationEntry) return null
            parent = parent.getParent()
        }
        return null
    }

    private fun countParenthesisBalance(at: PsiElement, container: PsiElement): Int {
        val stopAt = container.prevLeaf()
        var current: PsiElement? = at
        var balance = 0
        while (current != stopAt) {
            when (current!!.getNode().getElementType()) {
                JetTokens.LPAR -> balance++
                JetTokens.RPAR -> balance--
            }
            current = current!!.prevLeaf()
        }
        return balance
    }
}
