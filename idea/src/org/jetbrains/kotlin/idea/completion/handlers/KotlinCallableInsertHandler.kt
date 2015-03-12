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

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetImportDirective
import com.intellij.codeInsight.AutoPopupController
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.formatter.JetCodeStyleSettings
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.types.JetType
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.completion.DeclarationDescriptorLookupObject
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetParenthesizedExpression
import org.jetbrains.kotlin.psi.JetBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetTypeArgumentList
import com.intellij.codeInsight.lookup.Lookup
import org.jetbrains.kotlin.idea.completion.isAfterDot
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers

public abstract class KotlinCallableInsertHandler : BaseDeclarationInsertHandler() {
    public override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        addImport(context, item)
    }

    private fun addImport(context : InsertionContext, item : LookupElement) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments()

        ApplicationManager.getApplication()?.runReadAction { () : Unit ->
            val startOffset = context.getStartOffset()
            val element = context.getFile().findElementAt(startOffset)

            if (element == null) return@runReadAction

            val file = context.getFile()
            val o = item.getObject()
            if (file is JetFile && o is DeclarationDescriptorLookupObject) {
                val descriptor = o.descriptor as? CallableDescriptor
                if (descriptor != null) {
                    // for completion after dot, import insertion may be required only for extensions
                    if (context.isAfterDot() && descriptor.getExtensionReceiverParameter() == null) {
                        return@runReadAction
                    }

                    if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
                        ApplicationManager.getApplication()?.runWriteAction {
                            ImportInsertHelper.getInstance(context.getProject()).importDescriptor(file, descriptor)
                        }
                    }
                }
            }
        }
    }
}

public object KotlinPropertyInsertHandler : KotlinCallableInsertHandler()

public enum class CaretPosition {
    IN_BRACKETS
    AFTER_BRACKETS
}

public data class GenerateLambdaInfo(val lambdaType: JetType, val explicitParameters: Boolean)

public class KotlinFunctionInsertHandler(val caretPosition : CaretPosition, val lambdaInfo: GenerateLambdaInfo?) : KotlinCallableInsertHandler() {
    {
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
            offset = skipSpaces(chars, offset)
            if (offset < document.getTextLength()) {
                if (chars[offset] == '<') {
                    PsiDocumentManager.getInstance(context.getProject()).commitDocument(document)
                    val psiFile = context.getFile()
                    val token = psiFile.findElementAt(offset)
                    if (token.getNode().getElementType() == JetTokens.LT) {
                        val parent = token.getParent()
                        if (parent is JetTypeArgumentList && parent.getText().indexOf('\n') < 0/* if type argument list is on multiple lines this is more likely wrong parsing*/) {
                            offset = parent.getTextRange().getEndOffset()
                        }
                    }
                }
            }
        }

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
        assert(openingBracketOffset != -1, "If there wasn't open bracket it should already have been inserted")

        val closeBracketOffset = indexOfSkippingSpace(document, closingBracket, openingBracketOffset + 1)
        val editor = context.getEditor()

        if (shouldPlaceCaretInBrackets(completionChar) || closeBracketOffset == -1) {
            editor.getCaretModel().moveToOffset(openingBracketOffset + 1 + inBracketsShift)
            AutoPopupController.getInstance(context.getProject())?.autoPopupParameterInfo(editor, offsetElement)
        }
        else {
            editor.getCaretModel().moveToOffset(closeBracketOffset + 1)
        }

        PsiDocumentManager.getInstance(context.getProject()).commitDocument(document)

        if (lambdaInfo != null && lambdaInfo.explicitParameters) {
            insertLambdaTemplate(context, TextRange(openingBracketOffset, closeBracketOffset + 1), lambdaInfo.lambdaType)
        }
    }

    private fun shouldPlaceCaretInBrackets(completionChar: Char): Boolean {
        if (completionChar == ',' || completionChar == '.' || completionChar == '=') return false
        if (completionChar == '(') return true
        return caretPosition == CaretPosition.IN_BRACKETS
    }

    default object {
        public val NO_PARAMETERS_HANDLER: KotlinFunctionInsertHandler = KotlinFunctionInsertHandler(CaretPosition.AFTER_BRACKETS, null)
        public val WITH_PARAMETERS_HANDLER: KotlinFunctionInsertHandler = KotlinFunctionInsertHandler(CaretPosition.IN_BRACKETS, null)

        private fun shouldAddBrackets(element : PsiElement) : Boolean {
            return element.getStrictParentOfType<JetImportDirective>() == null
        }

        private fun indexOfSkippingSpace(document: Document, ch : Char, startIndex : Int) : Int {
            val text = document.getCharsSequence()
            for (i in startIndex..text.length() - 1) {
                val currentChar = text[i]
                if (ch == currentChar) return i
                if (currentChar != ' ' && currentChar != '\t') return -1
            }
            return -1
        }

        private fun skipSpaces(chars: CharSequence, index : Int) : Int
                = (index..chars.length() - 1).firstOrNull { val c = chars[it]; c != ' ' && c != '\t' } ?: chars.length()

        private fun isInsertSpacesInOneLineFunctionEnabled(project : Project)
                = CodeStyleSettingsManager.getSettings(project)
                      .getCustomSettings(javaClass<JetCodeStyleSettings>())!!.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD
    }
}

object CastReceiverInsertHandler : KotlinCallableInsertHandler() {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        val expression = PsiTreeUtil.findElementOfClassAtOffset(context.getFile(), context.getStartOffset(), javaClass<JetSimpleNameExpression>(), false)
        val qualifiedExpression = PsiTreeUtil.getParentOfType(expression, javaClass<JetQualifiedExpression>(), true)
        if (qualifiedExpression != null) {
            val receiver = qualifiedExpression.getReceiverExpression()

            val descriptor = (item.getObject() as? DeclarationDescriptorLookupObject)?.descriptor as CallableDescriptor
            val project = context.getProject()

            val thisObj = if (descriptor.getExtensionReceiverParameter() != null) descriptor.getExtensionReceiverParameter() else descriptor.getDispatchReceiverParameter()
            val fqName = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(thisObj.getType().getConstructor().getDeclarationDescriptor())

            val parentCast = JetPsiFactory(project).createExpression("(expr as $fqName)") as JetParenthesizedExpression
            val cast = parentCast.getExpression() as JetBinaryExpressionWithTypeRHS
            cast.getLeft().replace(receiver)

            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            psiDocumentManager.commitAllDocuments()
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.getDocument())

            val expr = receiver.replace(parentCast) as JetParenthesizedExpression

            ShortenReferences.DEFAULT.process((expr.getExpression() as JetBinaryExpressionWithTypeRHS).getRight())
        }
    }
}
