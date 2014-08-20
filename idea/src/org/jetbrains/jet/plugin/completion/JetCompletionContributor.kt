/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.completion.smart.SmartCompletion

import com.intellij.patterns.PsiJavaPatterns.elementType
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.lang.psi.JetTypeReference

public class JetCompletionContributor : CompletionContributor() {

    private val AFTER_NUMBER_LITERAL = psiElement().afterLeafSkipping(psiElement().withText(""), psiElement().withElementType(elementType().oneOf(JetTokens.FLOAT_LITERAL, JetTokens.INTEGER_LITERAL)))

    private val EXTENSION_RECEIVER_TYPE_DUMMY_IDENTIFIER = "KotlinExtensionDummy.fake() {}" // A way to add reference into file at completion place
    private val EXTENSION_RECEIVER_TYPE_ACTIVATION_PATTERN = psiElement().afterLeaf(JetTokens.FUN_KEYWORD.toString(), JetTokens.VAL_KEYWORD.toString(), JetTokens.VAR_KEYWORD.toString())

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
            context.getCompletionType() == CompletionType.SMART -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$" // add '$' to ignore context after the caret

            PackageDirectiveCompletion.ACTIVATION_PATTERN.accepts(tokenBefore) -> PackageDirectiveCompletion.DUMMY_IDENTIFIER

            EXTENSION_RECEIVER_TYPE_ACTIVATION_PATTERN.accepts(tokenBefore) -> EXTENSION_RECEIVER_TYPE_DUMMY_IDENTIFIER

            tokenBefore != null && isExtensionReceiverAfterDot(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "."

            else -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
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
                }
            }
        }
    }

    private val declarationKeywords = setOf(JetTokens.FUN_KEYWORD, JetTokens.VAL_KEYWORD, JetTokens.VAR_KEYWORD)

    private fun isExtensionReceiverAfterDot(tokenBefore: PsiElement): Boolean {
        var prev = tokenBefore.getPrevSibling()
        if (tokenBefore.getNode()!!.getElementType() != JetTokens.DOT) {
            if (prev == null || prev!!.getNode()!!.getElementType() != JetTokens.DOT) return false
            prev = prev!!.getPrevSibling()
        }

        while (prev != null) {
            if (prev!!.getNode()!!.getElementType() in declarationKeywords) {
                return true
            }
            if (prev !is PsiComment && prev !is PsiWhiteSpace && prev !is JetTypeReference) return false
            prev = prev!!.getPrevSibling()
        }
        return false
    }

    private fun performCompletion(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.getPosition()
        if (position.getContainingFile() !is JetFile) return

        if (AFTER_NUMBER_LITERAL.accepts(parameters.getPosition())) {
            // First Kotlin completion contributors - stop here will stop all completion
            result.stopHere()
            return
        }

        if (EXTENSION_RECEIVER_TYPE_ACTIVATION_PATTERN.accepts(position) && parameters.getInvocationCount() == 0) { // no auto-popup on typing after "val", "var" and "fun"
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
                val somethingAdded = BasicCompletionSession(configuration, parameters, result).complete()
                if (!somethingAdded && parameters.getInvocationCount() < 2) {
                    // Rerun completion if nothing was found
                    val newConfiguration = CompletionSessionConfiguration(completeNonImportedDeclarations = true, completeNonAccessibleDeclarations = parameters.getInvocationCount() > 0)
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

    private fun isAtEndOfLine(offset: Int, document: Document): Boolean {
        var i = offset
        val chars = document.getCharsSequence()
        while (i < chars.length()) {
            val c = chars.charAt(i)
            if (c == '\n' || c == 'r') return true
            if (!Character.isWhitespace(c)) return false
            i++
        }
        return true
    }
}
