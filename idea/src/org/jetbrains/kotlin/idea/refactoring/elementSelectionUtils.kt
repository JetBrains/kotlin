/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupAdapter
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.refactoring.introduce.findExpressionOrStringFragment
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.awt.Component
import java.lang.RuntimeException
import java.util.*
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JList

@Throws(IntroduceRefactoringException::class)
fun selectElement(
        editor: Editor,
        file: KtFile,
        elementKinds: Collection<CodeInsightUtils.ElementKind>,
        callback: (PsiElement?) -> Unit
) = selectElement(editor, file, true, elementKinds, callback)

@Throws(IntroduceRefactoringException::class)
fun selectElement(editor: Editor,
                  file: KtFile,
                  failOnEmptySuggestion: Boolean,
                  elementKinds: Collection<CodeInsightUtils.ElementKind>,
                  callback: (PsiElement?) -> Unit) {
    if (editor.selectionModel.hasSelection()) {
        var selectionStart = editor.selectionModel.selectionStart
        var selectionEnd = editor.selectionModel.selectionEnd

        var firstElement: PsiElement = file.findElementAt(selectionStart)!!
        var lastElement: PsiElement = file.findElementAt(selectionEnd - 1)!!

        if (PsiTreeUtil.getParentOfType(firstElement, KtLiteralStringTemplateEntry::class.java, KtEscapeStringTemplateEntry::class.java) == null
            && PsiTreeUtil.getParentOfType(lastElement, KtLiteralStringTemplateEntry::class.java, KtEscapeStringTemplateEntry::class.java) == null) {
            firstElement = firstElement.getNextSiblingIgnoringWhitespaceAndComments(true)!!
            lastElement = lastElement.getPrevSiblingIgnoringWhitespaceAndComments(true)!!
            selectionStart = firstElement.textRange.startOffset
            selectionEnd = lastElement.textRange.endOffset
        }

        val element = elementKinds.asSequence()
                .mapNotNull { findElement(file, selectionStart, selectionEnd, failOnEmptySuggestion, it) }
                .firstOrNull()
        callback(element)
    }
    else {
        val offset = editor.caretModel.offset
        smartSelectElement(editor, file, offset, failOnEmptySuggestion, elementKinds, callback)
    }
}

@Throws(IntroduceRefactoringException::class)
fun getSmartSelectSuggestions(
        file: PsiFile,
        offset: Int,
        elementKind: CodeInsightUtils.ElementKind
): List<KtElement> {
    if (offset < 0) return emptyList()

    var element: PsiElement? = file.findElementAt(offset) ?: return emptyList()

    if (element is PsiWhiteSpace) return getSmartSelectSuggestions(file, offset - 1, elementKind)

    val elements = ArrayList<KtElement>()
    while (element != null && !(element is KtBlockExpression && element.parent !is KtFunctionLiteral) &&
           element !is KtNamedFunction
           && element !is KtClassBody) {
        var addElement = false
        var keepPrevious = true

        if (element is KtTypeElement) {
            addElement =
                    elementKind == CodeInsightUtils.ElementKind.TYPE_ELEMENT
                    && element.getParentOfTypeAndBranch<KtUserType>(true) { qualifier } == null
            if (!addElement) {
                keepPrevious = false
            }
        }
        else if (element is KtExpression && element !is KtStatementExpression) {
            addElement = elementKind == CodeInsightUtils.ElementKind.EXPRESSION

            if (addElement) {
                if (element is KtParenthesizedExpression) {
                    addElement = false
                }
                else if (KtPsiUtil.isLabelIdentifierExpression(element)) {
                    addElement = false
                }
                else if (element.parent is KtQualifiedExpression) {
                    val qualifiedExpression = element.parent as KtQualifiedExpression
                    if (qualifiedExpression.receiverExpression !== element) {
                        addElement = false
                    }
                }
                else if (element.parent is KtCallElement
                         || element.parent is KtThisExpression
                         || PsiTreeUtil.getParentOfType(element, KtSuperExpression::class.java) != null) {
                    addElement = false
                }
                else if (element.parent is KtOperationExpression) {
                    val operationExpression = element.parent as KtOperationExpression
                    if (operationExpression.operationReference === element) {
                        addElement = false
                    }
                }
                if (addElement) {
                    val bindingContext = element.analyze(BodyResolveMode.FULL)
                    val expressionType = bindingContext.getType(element)
                    if (expressionType == null || KotlinBuiltIns.isUnit(expressionType)) {
                        addElement = false
                    }
                }
            }
        }

        if (addElement) {
            elements.add(element as KtElement)
        }

        if (!keepPrevious) {
            elements.clear()
        }

        element = element.parent
    }
    return elements
}

@Throws(IntroduceRefactoringException::class)
private fun smartSelectElement(
        editor: Editor,
        file: PsiFile,
        offset: Int,
        failOnEmptySuggestion: Boolean,
        elementKinds: Collection<CodeInsightUtils.ElementKind>,
        callback: (PsiElement?) -> Unit
) {
    val elements = elementKinds.flatMap { getSmartSelectSuggestions(file, offset, it) }
    if (elements.isEmpty()) {
        if (failOnEmptySuggestion) throw IntroduceRefactoringException(KotlinRefactoringBundle.message("cannot.refactor.not.expression"))
        callback(null)
        return
    }

    if (elements.size == 1 || ApplicationManager.getApplication().isUnitTestMode) {
        callback(elements.first())
        return
    }

    val model = DefaultListModel<PsiElement>()
    elements.forEach { model.addElement(it) }

    val highlighter = ScopeHighlighter(editor)

    val list = JBList(model)

    list.cellRenderer = object : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
            val rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            val element = value as KtElement?
            if (element!!.isValid) {
                text = getExpressionShortText(element)
            }
            return rendererComponent
        }
    }

    list.addListSelectionListener {
        highlighter.dropHighlight()
        val selectedIndex = list.selectedIndex
        if (selectedIndex < 0) return@addListSelectionListener
        val toExtract = ArrayList<PsiElement>()
        toExtract.add(model.get(selectedIndex))
        highlighter.highlight(model.get(selectedIndex), toExtract)
    }

    var title = "Elements"
    if (elementKinds.size == 1) {
        when (elementKinds.iterator().next()) {
            CodeInsightUtils.ElementKind.EXPRESSION -> title = "Expressions"
            CodeInsightUtils.ElementKind.TYPE_ELEMENT, CodeInsightUtils.ElementKind.TYPE_CONSTRUCTOR -> title = "Types"
        }
    }

    JBPopupFactory.getInstance()
            .createListPopupBuilder(list)
            .setTitle(title)
            .setMovable(false)
            .setResizable(false)
            .setRequestFocus(true)
            .setItemChoosenCallback { callback(list.selectedValue as KtElement) }
            .addListener(
                    object : JBPopupAdapter() {
                        override fun onClosed(event: LightweightWindowEvent?) {
                            highlighter.dropHighlight()
                        }
                    }
            )
            .createPopup()
            .showInBestPositionFor(editor)
}

fun getExpressionShortText(element: KtElement): String {
    val text = element.renderTrimmed()
    val firstNewLinePos = text.indexOf('\n')
    var trimmedText = text.substring(0, if (firstNewLinePos != -1) firstNewLinePos else Math.min(100, text.length))
    if (trimmedText.length != text.length) trimmedText += " ..."
    return trimmedText
}

@Throws(IntroduceRefactoringException::class)
private fun findElement(
        file: KtFile,
        startOffset: Int,
        endOffset: Int,
        failOnNoExpression: Boolean,
        elementKind: CodeInsightUtils.ElementKind
): PsiElement? {
    var element = CodeInsightUtils.findElement(file, startOffset, endOffset, elementKind)
    if (element == null && elementKind == CodeInsightUtils.ElementKind.EXPRESSION) {
        element = findExpressionOrStringFragment(file, startOffset, endOffset)
    }
    if (element == null) {
        //todo: if it's infix expression => add (), then commit document then return new created expression

        if (failOnNoExpression) {
            throw IntroduceRefactoringException(KotlinRefactoringBundle.message("cannot.refactor.not.expression"))
        }
        return null
    }
    return element
}

class IntroduceRefactoringException(message: String) : RuntimeException(message)
