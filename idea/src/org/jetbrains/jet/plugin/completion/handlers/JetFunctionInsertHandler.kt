/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetImportDirective
import com.intellij.codeInsight.AutoPopupController
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.openapi.project.Project
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper
import com.intellij.openapi.editor.Document
import org.jetbrains.jet.lang.types.JetType
import com.intellij.openapi.util.TextRange
import org.jetbrains.jet.plugin.completion.DeclarationLookupObject

public enum class CaretPosition {
    IN_BRACKETS
    AFTER_BRACKETS
}

public data class GenerateLambdaInfo(val lambdaType: JetType, val explicitParameters: Boolean)

public class JetFunctionInsertHandler(val caretPosition : CaretPosition, val lambdaInfo: GenerateLambdaInfo?) : InsertHandler<LookupElement> {
    {
        if (caretPosition == CaretPosition.AFTER_BRACKETS && lambdaInfo != null) {
            throw IllegalArgumentException("CaretPosition.AFTER_BRACKETS with lambdaInfo != null combination is not supported")
        }
    }

    public override fun handleInsert(context: InsertionContext, item: LookupElement) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments()
        if (context.getCompletionChar() == '(') {
            context.setAddCompletionChar(false)
        }

        val startOffset = context.getStartOffset()
        val element = context.getFile().findElementAt(startOffset)

        if (element == null) return

        if (shouldAddBrackets(element)) {
            addBrackets(context, element)
        }

        addImports(context, item)
    }

    private fun addBrackets(context : InsertionContext, offsetElement : PsiElement) {
        val offset = context.getTailOffset()
        val document = context.getDocument()
        val completionChar = context.getCompletionChar()

        val forceParenthesis = lambdaInfo != null && completionChar == '\t' && document.getCharsSequence().charAt(offset) == '('
        val braces = lambdaInfo != null && completionChar != '(' && !forceParenthesis

        val openingBracket = if (braces) '{' else '('
        val closingBracket = if (braces) '}' else ')'

        var openingBracketOffset = indexOfSkippingSpace(document, openingBracket, offset)
        var inBracketsShift = 0
        if (openingBracketOffset == -1) {
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

        openingBracketOffset = indexOfSkippingSpace(document, openingBracket, offset)
        assert (openingBracketOffset != -1) { "If there wasn't open bracket it should already have been inserted" }

        val closeBracketOffset = indexOfSkippingSpace(document, closingBracket, openingBracketOffset + 1)
        val editor = context.getEditor()

        var forcePlaceCaretIntoParentheses : Boolean = completionChar == '('

        if (caretPosition == CaretPosition.IN_BRACKETS || forcePlaceCaretIntoParentheses || closeBracketOffset == -1) {
            editor.getCaretModel().moveToOffset(openingBracketOffset + 1 + inBracketsShift)
            AutoPopupController.getInstance(context.getProject())?.autoPopupParameterInfo(editor, offsetElement)
        }
        else {
            editor.getCaretModel().moveToOffset(closeBracketOffset + 1)
        }

        PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getDocument())

        if (lambdaInfo != null && lambdaInfo.explicitParameters) {
            insertLambdaTemplate(context, TextRange(openingBracketOffset, closeBracketOffset + 1), lambdaInfo.lambdaType)
        }
    }

    class object {
        public val NO_PARAMETERS_HANDLER: JetFunctionInsertHandler = JetFunctionInsertHandler(CaretPosition.AFTER_BRACKETS, null)
        public val WITH_PARAMETERS_HANDLER: JetFunctionInsertHandler = JetFunctionInsertHandler(CaretPosition.IN_BRACKETS, null)

        private fun shouldAddBrackets(element : PsiElement) : Boolean {
            return PsiTreeUtil.getParentOfType(element, javaClass<JetImportDirective>()) == null
        }

        private fun indexOfSkippingSpace(document: Document, ch : Char, startIndex : Int) : Int {
            val text = document.getCharsSequence()
            for (i in startIndex..text.length() - 1) {
                val currentChar = text.charAt(i)
                if (ch == currentChar) return i
                if (!Character.isWhitespace(currentChar)) return -1
            }
            return -1
        }

        private open fun isInsertSpacesInOneLineFunctionEnabled(project : Project)
                = CodeStyleSettingsManager.getSettings(project)
                      .getCustomSettings(javaClass<JetCodeStyleSettings>())!!.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD

        private open fun addImports(context : InsertionContext, item : LookupElement) {
            ApplicationManager.getApplication()?.runReadAction { () : Unit ->
                val startOffset = context.getStartOffset()
                val element = context.getFile().findElementAt(startOffset)

                if (element == null) return@runReadAction

                val file = context.getFile()
                val o = item.getObject()
                if (file is JetFile && o is DeclarationLookupObject) {
                    val descriptor = o.descriptor as? SimpleFunctionDescriptor
                    if (descriptor != null) {

                        if (PsiTreeUtil.getParentOfType(element, javaClass<JetQualifiedExpression>()) != null &&
                                descriptor.getReceiverParameter() == null) {
                            return@runReadAction
                        }

                        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
                            ApplicationManager.getApplication()?.runWriteAction {
                                val fqn = DescriptorUtils.getFqNameSafe(descriptor)
                                ImportInsertHelper.addImportDirectiveIfNeeded(fqn, file)
                            }
                        }
                    }
                }
            }
        }
    }
}