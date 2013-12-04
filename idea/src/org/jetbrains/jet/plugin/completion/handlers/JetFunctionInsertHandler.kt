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
import org.jetbrains.jet.plugin.completion.JetLookupObject
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper

public enum class CaretPosition {
    IN_BRACKETS
    AFTER_BRACKETS
}
public enum class BracketType {
    PARENTHESIS
    BRACES
}

public class JetFunctionInsertHandler(val caretPosition : CaretPosition, val bracketType : BracketType) : InsertHandler<LookupElement> {
    {
        if (caretPosition == CaretPosition.AFTER_BRACKETS && bracketType == BracketType.BRACES) {
            throw IllegalArgumentException("CaretPosition.AFTER_BRACKETS with bracketType == BracketType.BRACES combination is not supported")
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

        addImport(context, item)
    }

    private fun addBrackets(context : InsertionContext, offsetElement : PsiElement) {
        val offset = context.getSelectionEndOffset()
        val document = context.getDocument()
        val completionChar = context.getCompletionChar()

        var documentText = document.getText()

        val forceParenthesis = bracketType == BracketType.BRACES && completionChar == '\t' && documentText.charAt(offset) == '('

        val braces = bracketType == BracketType.BRACES && completionChar != '(' && !forceParenthesis

        val openingBracket = if (braces) '{' else '('
        val closingBracket = if (braces) '}' else ')'

        var openingBracketIndex = indexOfSkippingSpace(documentText, openingBracket, offset)
        var inBracketsShift = 0
        if (openingBracketIndex == -1) {
            if (braces) {
                if (completionChar == ' ' || completionChar == '{') {
                    context.setAddCompletionChar(false)
                }

                if (isInsertSpacesInOneLineFunctionEnabled(offsetElement.getProject())) {
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
            documentText = document.getText()
        }

        openingBracketIndex = indexOfSkippingSpace(documentText, openingBracket, offset)
        assert (openingBracketIndex != -1) { "If there wasn't open bracket it should already have been inserted" }

        val closeBracketIndex = indexOfSkippingSpace(documentText, closingBracket, openingBracketIndex + 1)
        val editor = context.getEditor()

        var forcePlaceCaretIntoParentheses : Boolean = completionChar == '('

        if (caretPosition == CaretPosition.IN_BRACKETS || forcePlaceCaretIntoParentheses || closeBracketIndex == -1) {
            editor.getCaretModel().moveToOffset(openingBracketIndex + 1 + inBracketsShift)
            AutoPopupController.getInstance(context.getProject())?.autoPopupParameterInfo(editor, offsetElement)
        }
        else {
            editor.getCaretModel().moveToOffset(closeBracketIndex + 1)
        }

        PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getDocument())
    }

    class object {
        public val EMPTY_FUNCTION_HANDLER: JetFunctionInsertHandler = JetFunctionInsertHandler(CaretPosition.AFTER_BRACKETS, BracketType.PARENTHESIS)
        public val PARAMS_PARENTHESIS_FUNCTION_HANDLER: JetFunctionInsertHandler = JetFunctionInsertHandler(CaretPosition.IN_BRACKETS, BracketType.PARENTHESIS)
        public val PARAMS_BRACES_FUNCTION_HANDLER: JetFunctionInsertHandler = JetFunctionInsertHandler(CaretPosition.IN_BRACKETS, BracketType.BRACES)

        private fun shouldAddBrackets(element : PsiElement) : Boolean {
            return PsiTreeUtil.getParentOfType(element, javaClass<JetImportDirective>()) == null
        }

        private fun indexOfSkippingSpace(str : String, ch : Char, startIndex : Int) : Int {
            for (i in startIndex..str.length() - 1) {
                val currentChar = str.charAt(i)
                if (ch == currentChar) {
                    return i
                }

                if (!Character.isWhitespace(currentChar)) {
                    return -1
                }
            }

            return -1
        }

        private open fun isInsertSpacesInOneLineFunctionEnabled(project : Project) : Boolean {
            val settings = CodeStyleSettingsManager.getSettings(project)
            val jetSettings = settings.getCustomSettings(javaClass<JetCodeStyleSettings>())!!
            return jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD
        }

        private open fun addImport(context : InsertionContext, item : LookupElement) {
            ApplicationManager.getApplication()?.runReadAction { () : Unit ->
                val startOffset = context.getStartOffset()
                val element = context.getFile().findElementAt(startOffset)

                if (element == null) return@runReadAction

                if (context.getFile() is JetFile && item.getObject() is JetLookupObject) {
                    val descriptor = (item.getObject() as JetLookupObject).getDescriptor()

                    if (descriptor is SimpleFunctionDescriptor) {
                        val file = context.getFile() as JetFile
                        val functionDescriptor = descriptor as SimpleFunctionDescriptor

                        if (PsiTreeUtil.getParentOfType(element, javaClass<JetQualifiedExpression>()) != null &&
                        functionDescriptor.getReceiverParameter() == null) {
                            return@runReadAction
                        }

                        if (DescriptorUtils.isTopLevelDeclaration(functionDescriptor)) {
                            ApplicationManager.getApplication()?.runWriteAction {
                                val fqn = DescriptorUtils.getFqNameSafe(functionDescriptor)
                                ImportInsertHelper.addImportDirectiveIfNeeded(fqn, file)
                            }
                        }
                    }
                }
            }
        }
    }
}