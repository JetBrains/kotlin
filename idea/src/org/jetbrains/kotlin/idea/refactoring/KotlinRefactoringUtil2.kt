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
import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupAdapter
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.psi.*
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.psi.util.PsiFormatUtilBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBList
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.refactoring.introduce.findExpressionOrStringFragment
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.awt.Component
import java.lang.RuntimeException
import java.util.*
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JList

fun wrapOrSkip(s: String, inCode: Boolean) = if (inCode) "<code>$s</code>" else s

fun formatClassDescriptor(classDescriptor: DeclarationDescriptor) = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(classDescriptor)

fun formatPsiClass(
        psiClass: PsiClass,
        markAsJava: Boolean,
        inCode: Boolean
): String {
    var description: String

    val kind = if (psiClass.isInterface) "interface " else "class "
    description = kind + PsiFormatUtil.formatClass(
            psiClass,
            PsiFormatUtilBase.SHOW_CONTAINING_CLASS or PsiFormatUtilBase.SHOW_NAME or PsiFormatUtilBase.SHOW_PARAMETERS or PsiFormatUtilBase.SHOW_TYPE
    )
    description = wrapOrSkip(description, inCode)

    return if (markAsJava) "[Java] $description" else description
}

fun checkSuperMethods(
        declaration: KtDeclaration,
        ignore: Collection<PsiElement>?,
        actionStringKey: String
): List<PsiElement> {
    val declarationDescriptor = declaration.resolveToDescriptor() as CallableDescriptor

    if (declarationDescriptor is LocalVariableDescriptor) return listOf(declaration)

    val project = declaration.project
    val overriddenElementsToDescriptor = HashMap<PsiElement, CallableDescriptor>()
    for (overriddenDescriptor in DescriptorUtils.getAllOverriddenDescriptors(declarationDescriptor)) {
        val overriddenDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, overriddenDescriptor) ?: continue
        if (overriddenDeclaration is KtNamedFunction || overriddenDeclaration is KtProperty || overriddenDeclaration is PsiMethod) {
            overriddenElementsToDescriptor[overriddenDeclaration] = overriddenDescriptor
        }
    }
    if (ignore != null) {
        overriddenElementsToDescriptor.keys.removeAll(ignore)
    }

    if (overriddenElementsToDescriptor.isEmpty()) return listOf(declaration)

    return askUserForMethodsToSearch(declaration, declarationDescriptor, overriddenElementsToDescriptor, actionStringKey)
}

private fun askUserForMethodsToSearch(
        declaration: KtDeclaration,
        declarationDescriptor: CallableDescriptor,
        overriddenElementsToDescriptor: Map<PsiElement, CallableDescriptor>,
        actionStringKey: String
): List<PsiElement> {
    if (ApplicationManager.getApplication().isUnitTestMode) return overriddenElementsToDescriptor.keys.toList()

    val superClassDescriptions = getClassDescriptions(overriddenElementsToDescriptor)

    val message = KotlinBundle.message(
            "x.overrides.y.in.class.list",
            DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.render(declarationDescriptor),
            "\n${superClassDescriptions.joinToString(separator = "")}",
            KotlinBundle.message(actionStringKey)
    )

    val exitCode = Messages.showYesNoCancelDialog(declaration.project, message, IdeBundle.message("title.warning"), Messages.getQuestionIcon())
    when (exitCode) {
        Messages.YES -> return overriddenElementsToDescriptor.keys.toList()
        Messages.NO -> return listOf(declaration)
        else -> return emptyList()
    }
}

private fun getClassDescriptions(overriddenElementsToDescriptor: Map<PsiElement, CallableDescriptor>): List<String> {
    return overriddenElementsToDescriptor.entries.map { entry ->
        val (element, descriptor) = entry
        val description = when (element) {
            is KtNamedFunction, is KtProperty -> formatClassDescriptor(descriptor.containingDeclaration)
            is PsiMethod -> {
                val psiClass = element.containingClass ?: error("Invalid element: ${element.getText()}")
                formatPsiClass(psiClass, true, false)
            }
            else -> error("Unexpected element: ${element.getElementTextWithContext()}")
        }
        "    $description\n"
    }
}

fun formatClass(classDescriptor: DeclarationDescriptor, inCode: Boolean): String {
    val element = DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor)
    return if (element is PsiClass) {
        formatPsiClass(element, false, inCode)
    }
    else {
        wrapOrSkip(formatClassDescriptor(classDescriptor), inCode)
    }
}

fun formatFunction(functionDescriptor: DeclarationDescriptor, inCode: Boolean): String {
    val element = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor)
    return if (element is PsiMethod) {
        formatPsiMethod(element, false, inCode)
    }
    else {
        wrapOrSkip(formatFunctionDescriptor(functionDescriptor), inCode)
    }
}

private fun formatFunctionDescriptor(functionDescriptor: DeclarationDescriptor) = DescriptorRenderer.COMPACT.render(functionDescriptor)

fun formatPsiMethod(
        psiMethod: PsiMethod,
        showContainingClass: Boolean,
        inCode: Boolean
): String {
    var options = PsiFormatUtilBase.SHOW_NAME or PsiFormatUtilBase.SHOW_PARAMETERS or PsiFormatUtilBase.SHOW_TYPE
    if (showContainingClass) {
        //noinspection ConstantConditions
        options = options or PsiFormatUtilBase.SHOW_CONTAINING_CLASS
    }

    var description = PsiFormatUtil.formatMethod(psiMethod, PsiSubstitutor.EMPTY, options, PsiFormatUtilBase.SHOW_TYPE)
    description = wrapOrSkip(description, inCode)

    return "[Java] $description"
}

fun formatJavaOrLightMethod(method: PsiMethod): String {
    val originalDeclaration = method.unwrapped
    return if (originalDeclaration is KtDeclaration) {
        formatFunctionDescriptor(originalDeclaration.resolveToDescriptor())
    }
    else {
        formatPsiMethod(method, false, false)
    }
}

fun formatClass(classOrObject: KtClassOrObject) = formatClassDescriptor(classOrObject.resolveToDescriptor() as ClassDescriptor)

fun checkParametersInMethodHierarchy(parameter: PsiParameter): Collection<PsiElement>? {
    val method = parameter.declarationScope as PsiMethod

    val parametersToDelete = collectParametersHierarchy(method, parameter)
    if (parametersToDelete.size <= 1 || ApplicationManager.getApplication().isUnitTestMode) return parametersToDelete

    val message = KotlinBundle.message("delete.param.in.method.hierarchy", formatJavaOrLightMethod(method))
    val exitCode = Messages.showOkCancelDialog(parameter.project, message, IdeBundle.message("title.warning"), Messages.getQuestionIcon())
    return if (exitCode == Messages.OK) parametersToDelete else null
}

// TODO: generalize breadth-first search
private fun collectParametersHierarchy(method: PsiMethod, parameter: PsiParameter): Set<PsiElement> {
    val queue = ArrayDeque<PsiMethod>()
    val visited = HashSet<PsiMethod>()
    val parametersToDelete = HashSet<PsiElement>()

    queue.add(method)
    while (!queue.isEmpty()) {
        val currentMethod = queue.poll()

        visited += currentMethod
        addParameter(currentMethod, parametersToDelete, parameter)

        currentMethod.findSuperMethods(true)
                .filter { it !in visited }
                .forEach { queue.offer(it) }
        OverridingMethodsSearch.search(currentMethod)
                .filter { it !in visited }
                .forEach { queue.offer(it) }
    }
    return parametersToDelete
}

private fun addParameter(method: PsiMethod, result: MutableSet<PsiElement>, parameter: PsiParameter) {
    val parameterIndex = parameter.unwrapped!!.parameterIndex()

    if (method is KtLightMethod) {
        val declaration = method.kotlinOrigin
        if (declaration is KtFunction) {
            result.add(declaration.valueParameters[parameterIndex])
        }
    }
    else {
        result.add(method.parameterList.parameters[parameterIndex])
    }
}

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
            addElement = elementKind == CodeInsightUtils.ElementKind.TYPE_ELEMENT
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
