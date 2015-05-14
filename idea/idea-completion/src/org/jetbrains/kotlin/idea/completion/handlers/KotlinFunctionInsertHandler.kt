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

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.core.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetTypeArgumentList
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.types.JetType

enum class CaretPosition {
    IN_BRACKETS
    AFTER_BRACKETS
}

data class GenerateLambdaInfo(val lambdaType: JetType, val explicitParameters: Boolean)

class KotlinFunctionInsertHandler(val caretPosition : CaretPosition, val lambdaInfo: GenerateLambdaInfo?) : KotlinCallableInsertHandler() {
    init {
        if (caretPosition == CaretPosition.AFTER_BRACKETS && lambdaInfo != null) {
            throw IllegalArgumentException("CaretPosition.AFTER_BRACKETS with lambdaInfo != null combination is not supported")
        }
    }

    public override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        val psiDocumentManager = PsiDocumentManager.getInstance(context.getProject())
        psiDocumentManager.commitAllDocuments()
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.getDocument())

        val startOffset = context.getStartOffset()
        val element = context.getFile().findElementAt(startOffset) ?: return

        when {
            element.getStrictParentOfType<JetImportDirective>() != null -> return

            isInfixCall(element) -> {
                if (context.getCompletionChar() == ' ') {
                    context.setAddCompletionChar(false)
                }

                val tailOffset = context.getTailOffset()
                context.getDocument().insertString(tailOffset, " ")
                context.getEditor().getCaretModel().moveToOffset(tailOffset + 1)
            }

            else -> addBrackets(context, element)
        }
    }

    private fun isInfixCall(context: PsiElement): Boolean {
        val parent = context.getParent()
        val grandParent = parent?.getParent()
        return parent is JetSimpleNameExpression && grandParent is JetBinaryExpression && parent == grandParent.getOperationReference()
    }

    private fun addBrackets(context : InsertionContext, offsetElement : PsiElement) {
        val completionChar = context.getCompletionChar()
        if (completionChar == '(') { //TODO: more correct behavior related to braces type
            context.setAddCompletionChar(false)
        }

        var offset = context.getTailOffset()
        val document = context.getDocument()
        val chars = document.getCharsSequence()

        val forceParenthesis = lambdaInfo != null && completionChar == '\t' && chars.charAt(offset) == '('
        val braces = lambdaInfo != null && completionChar != '(' && !forceParenthesis

        val openingBracket = if (braces) '{' else '('
        val closingBracket = if (braces) '}' else ')'

        if (completionChar == Lookup.REPLACE_SELECT_CHAR) {
            val offset1 = chars.skipSpaces(offset)
            if (offset1 < chars.length()) {
                if (chars[offset1] == '<') {
                    PsiDocumentManager.getInstance(context.getProject()).commitDocument(document)
                    val token = context.getFile().findElementAt(offset1)!!
                    if (token.getNode().getElementType() == JetTokens.LT) {
                        val parent = token.getParent()
                        if (parent is JetTypeArgumentList && parent.getText().indexOf('\n') < 0/* if type argument list is on multiple lines this is more likely wrong parsing*/) {
                            offset = parent.endOffset
                        }
                    }
                }
            }
        }

        var openingBracketOffset = chars.indexOfSkippingSpace(openingBracket, offset)
        var inBracketsShift = 0
        if (openingBracketOffset == null) {
            if (braces) {
                if (completionChar == ' ' || completionChar == '{') {
                    context.setAddCompletionChar(false)
                }

                if (isInsertSpacesInOneLineFunctionEnabled(context.getProject())) {
                    document.insertString(offset, " {  }")
                    inBracketsShift = 1
                }
                else {
                    document.insertString(offset, " {}")
                }
            }
            else {
                document.insertString(offset, "()")
            }
            PsiDocumentManager.getInstance(context.getProject()).commitDocument(document)
        }

        openingBracketOffset = chars.indexOfSkippingSpace(openingBracket, offset)!!

        val closeBracketOffset = chars.indexOfSkippingSpace(closingBracket, openingBracketOffset + 1)

        val editor = context.getEditor()
        if (shouldPlaceCaretInBrackets(completionChar) || closeBracketOffset == null) {
            editor.getCaretModel().moveToOffset(openingBracketOffset + 1 + inBracketsShift)
            AutoPopupController.getInstance(context.getProject())?.autoPopupParameterInfo(editor, offsetElement)
        }
        else {
            editor.getCaretModel().moveToOffset(closeBracketOffset + 1)
        }

        PsiDocumentManager.getInstance(context.getProject()).commitDocument(document)

        if (lambdaInfo != null && lambdaInfo.explicitParameters) {
            insertLambdaTemplate(context, TextRange(openingBracketOffset, closeBracketOffset!! + 1), lambdaInfo.lambdaType)
        }
    }

    private fun shouldPlaceCaretInBrackets(completionChar: Char): Boolean {
        if (completionChar == ',' || completionChar == '.' || completionChar == '=') return false
        if (completionChar == '(') return true
        return caretPosition == CaretPosition.IN_BRACKETS
    }

    companion object {
        public val NO_PARAMETERS_HANDLER: KotlinFunctionInsertHandler = KotlinFunctionInsertHandler(CaretPosition.AFTER_BRACKETS, null)
        public val WITH_PARAMETERS_HANDLER: KotlinFunctionInsertHandler = KotlinFunctionInsertHandler(CaretPosition.IN_BRACKETS, null)

        private fun isInsertSpacesInOneLineFunctionEnabled(project: Project)
                = CodeStyleSettingsManager.getSettings(project).getCustomSettings(javaClass<JetCodeStyleSettings>())!!.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD
    }
}