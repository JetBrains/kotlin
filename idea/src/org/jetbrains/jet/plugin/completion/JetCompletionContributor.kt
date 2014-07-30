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
import org.jetbrains.jet.plugin.references.JetSimpleNameReference

import com.intellij.patterns.PsiJavaPatterns.elementType
import com.intellij.patterns.PsiJavaPatterns.psiElement

public class JetCompletionContributor : CompletionContributor() {

    {
        val provider = object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                doSimpleReferenceCompletion(parameters, result)
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
        if (context.getCompletionType() == CompletionType.SMART) {
            context.setDummyIdentifier(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$") // add '$' to ignore context after the caret
        }
        else {
            if (JetPackagesContributor.ACTIVATION_PATTERN.accepts(tokenBefore)) {
                context.setDummyIdentifier(JetPackagesContributor.DUMMY_IDENTIFIER)
            }
            else if (JetExtensionReceiverTypeContributor.ACTIVATION_PATTERN.accepts(tokenBefore)) {
                context.setDummyIdentifier(JetExtensionReceiverTypeContributor.DUMMY_IDENTIFIER)
            }
            else {
                context.setDummyIdentifier(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
            }
        }

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

    class object {
        private val AFTER_NUMBER_LITERAL = psiElement().afterLeafSkipping(psiElement().withText(""), psiElement().withElementType(elementType().oneOf(JetTokens.FLOAT_LITERAL, JetTokens.INTEGER_LITERAL)))

        public fun doSimpleReferenceCompletion(parameters: CompletionParameters, result: CompletionResultSet) {
            val position = parameters.getPosition()
            if (position.getContainingFile() !is JetFile) return

            if (AFTER_NUMBER_LITERAL.accepts(parameters.getPosition())) {
                // First Kotlin completion contributors - stop here will stop all completion
                result.stopHere()
                return
            }

            val jetReference = getJetReference(parameters)
            if (jetReference != null) {
                try {
                    result.restartCompletionWhenNothingMatches()

                    if (parameters.getCompletionType() == CompletionType.BASIC) {
                        val somethingAdded = BasicCompletionSession(parameters, result, jetReference).complete()
                        if (!somethingAdded && parameters.getInvocationCount() < 2) {
                            // Rerun completion if nothing was found
                            BasicCompletionSession(parameters.withInvocationCount(2), result, jetReference).complete()
                        }
                    }
                    else {
                        SmartCompletionSession(parameters, result, jetReference).complete()
                    }
                }
                catch (e: ProcessCanceledException) {
                    throw rethrowWithCancelIndicator(e)
                }
            }
        }

        private fun getJetReference(parameters: CompletionParameters): JetSimpleNameReference?
                = parameters.getPosition().getParent()?.getReferences()?.filterIsInstance(javaClass<JetSimpleNameReference>())?.firstOrNull()

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
}
