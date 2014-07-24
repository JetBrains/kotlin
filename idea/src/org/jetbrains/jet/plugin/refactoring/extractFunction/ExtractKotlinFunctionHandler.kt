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

package org.jetbrains.jet.plugin.refactoring.extractFunction

import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiFile
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.ScrollType
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils
import org.jetbrains.jet.plugin.refactoring.JetRefactoringUtil
import com.intellij.refactoring.HelpID
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle
import org.jetbrains.jet.plugin.refactoring.extractFunction.ui.KotlinExtractFunctionDialog
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.refactoring.getAllExtractionContainers
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.lang.psi.psiUtil.getOutermostParentContainedIn
import org.jetbrains.jet.plugin.refactoring.checkConflictsInteractively
import org.jetbrains.jet.plugin.refactoring.executeWriteCommand
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetClassBody
import kotlin.test.fail
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult.Status
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.MessageType
import javax.swing.event.HyperlinkEvent
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.ui.awt.RelativePoint
import com.intellij.openapi.ui.popup.Balloon.Position
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetDeclaration
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetParameterList
import org.jetbrains.jet.lang.psi.JetClassInitializer
import org.jetbrains.jet.lang.psi.JetFunctionLiteral
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.util.text.StringUtil
import javax.swing.Icon
import org.jetbrains.jet.plugin.refactoring.getPsiElementPopup
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.plugin.util.collapseSpaces
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.psi.JetClassOrObject

public class ExtractKotlinFunctionHandler(public val allContainersEnabled: Boolean = false) : RefactoringActionHandler {
    fun doInvoke(
            editor: Editor,
            file: JetFile,
            elements: List<PsiElement>,
            targetSibling: PsiElement,
            preprocessor: ((ExtractionDescriptor) -> Unit)? = null
    ) {
        val project = file.getProject()

        val analysisResult = ExtractionData(file, elements, targetSibling).performAnalysis()

        if (ApplicationManager.getApplication()!!.isUnitTestMode() && analysisResult.status != Status.SUCCESS) {
            throw ConflictsInTestsException(analysisResult.messages.map { it.renderMessage() })
        }

        fun doRefactor(descriptor: ExtractionDescriptor) {
            preprocessor?.invoke(descriptor)
            project.executeWriteCommand(EXTRACT_FUNCTION) { descriptor.generateFunction() }
        }

        fun validateAndRefactor() {
            val validationResult = analysisResult.descriptor!!.validate()
            project.checkConflictsInteractively(validationResult.conflicts) {
                if (ApplicationManager.getApplication()!!.isUnitTestMode()) {
                    doRefactor(validationResult.descriptor)
                }
                else {
                    KotlinExtractFunctionDialog(project, validationResult) { doRefactor(it.getCurrentDescriptor()) }.show()
                }
            }
        }

        val message = analysisResult.messages.map { it.renderMessage() }.makeString("\n")
        when (analysisResult.status) {
            Status.CRITICAL_ERROR -> {
                showErrorHint(project, editor, message)
            }

            Status.NON_CRITICAL_ERROR -> {
                val anchorPoint = RelativePoint(
                        editor.getContentComponent(),
                        editor.visualPositionToXY(editor.getSelectionModel().getSelectionStartPosition()!!)
                )
                JBPopupFactory.getInstance()!!
                        .createHtmlTextBalloonBuilder(
                                "$message<br/><br/><a href=\"EXTRACT\">Proceed with extraction</a>",
                                MessageType.WARNING,
                                { event ->
                                    if (event?.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                        validateAndRefactor()
                                    }
                                }
                        )
                        .setHideOnClickOutside(true)
                        .setHideOnFrameResize(false)
                        .setHideOnLinkClick(true)
                        .createBalloon()
                        .show(anchorPoint, Position.below)
            }

            Status.SUCCESS -> validateAndRefactor()
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is JetFile) return

        selectElements(editor, file, allContainersEnabled) { (elements, targetSibling) ->
            doInvoke(editor, file, elements, targetSibling)
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        fail("Extract Function can only be invoked from editor")
    }
}

private val EXTRACT_FUNCTION: String = JetRefactoringBundle.message("extract.function")!!

private fun showErrorHint(project: Project, editor: Editor, message: String) {
    CodeInsightUtils.showErrorHint(project, editor, message, EXTRACT_FUNCTION, HelpID.EXTRACT_METHOD)
}

private fun showErrorHintByKey(project: Project, editor: Editor, key: String) {
    showErrorHint(project, editor, JetRefactoringBundle.message(key)!!)
}

fun selectElements(
        editor: Editor,
        file: PsiFile,
        allContainersEnabled: Boolean = false,
        continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit
) {
    fun noExpressionError() {
        showErrorHintByKey(file.getProject(), editor, "cannot.refactor.no.expression")
    }

    fun noContainerError() {
        showErrorHintByKey(file.getProject(), editor, "cannot.refactor.no.container")
    }

    fun onSelectionComplete(parent: PsiElement, elements: List<PsiElement>, targetContainer: JetElement) {
        if (parent == targetContainer) {
            continuation(elements, elements.first!!)
            return
        }

        val outermostParent = parent.getOutermostParentContainedIn(targetContainer)
        if (outermostParent == null) {
            noContainerError()
            return
        }

        continuation(elements, outermostParent)
    }

    fun getContainers(element: PsiElement, strict: Boolean): List<JetElement> {
        if (allContainersEnabled) return element.getAllExtractionContainers(strict)

        val declaration = element.getParentByType(javaClass<JetDeclaration>(), strict)?.let { declaration ->
            stream(declaration) { it.getParentByType(javaClass<JetDeclaration>(), true) }.firstOrNull { it !is JetFunctionLiteral }
        } ?: return Collections.emptyList()

        val parent = declaration.getParent()?.let {
            when (it) {
                is JetProperty -> it.getParent()
                is JetParameterList -> it.getParent()?.getParent()
                else -> it
            }
        }
        return when (parent) {
            is JetFile -> Collections.singletonList(parent)
            is JetClassBody -> {
                element.getAllExtractionContainers(strict).filterIsInstance(javaClass<JetClassBody>())
            }
            else -> {
                val enclosingDeclaration =
                        PsiTreeUtil.getNonStrictParentOfType(parent, javaClass<JetDeclarationWithBody>(), javaClass<JetClassInitializer>())
                val targetContainer = when (enclosingDeclaration) {
                    is JetDeclarationWithBody -> enclosingDeclaration.getBodyExpression()
                    is JetClassInitializer -> enclosingDeclaration.getBody()
                    else -> null
                }
                if (targetContainer is JetBlockExpression) Collections.singletonList(targetContainer) else Collections.emptyList()
            }
        }
    }

    fun selectTargetContainer(elements: List<PsiElement>) {
        val parent = PsiTreeUtil.findCommonParent(elements)
            ?: throw AssertionError("Should have at least one parent: ${elements.makeString("\n")}")

        val containers = getContainers(parent, elements.size == 1)
        if (containers.empty) {
            noContainerError()
            return
        }

        if (containers.size == 1 || ApplicationManager.getApplication()!!.isUnitTestMode()) {
            onSelectionComplete(parent, elements, containers[0])
            return
        }

        getPsiElementPopup(
                editor,
                containers.copyToArray(),
                object: PsiElementListCellRenderer<JetElement>() {
                    private fun JetElement.renderName(): String? {
                        if (this is JetPropertyAccessor) {
                            return (getParent() as JetProperty).renderName() + if (isGetter()) ".get" else ".set"
                        }
                        return (this as? PsiNamedElement)?.getName() ?: "<anonymous>"
                    }

                    private fun JetElement.renderDeclaration(): String? {
                        val name = renderName()
                        val descriptor = AnalyzerFacadeWithCache.getContextForElement(this)[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
                        val params = (descriptor as? FunctionDescriptor)?.let { descriptor ->
                            descriptor.getValueParameters()
                                    .map { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it.getType()) }
                                    .joinToString(", ", "(", ")")
                        } ?: ""
                        return "$name$params"
                    }

                    private fun JetElement.renderText(): String {
                        return StringUtil.shortenTextWithEllipsis(getText()!!.collapseSpaces(), 53, 0)
                    }

                    private fun JetElement.getRepresentativeElement(): JetElement {
                        return when (this) {
                            is JetBlockExpression -> (getParent() as? JetDeclarationWithBody) ?: this
                            is JetClassBody -> getParent() as JetClassOrObject
                            else -> this
                        }
                    }

                    override fun getElementText(element: JetElement): String? {
                        val representativeElement = element.getRepresentativeElement()
                        return when (representativeElement) {
                            is JetFile, is JetDeclarationWithBody, is JetClassOrObject -> representativeElement.renderDeclaration()
                            else -> representativeElement.renderText()
                        }
                    }

                    override fun getContainerText(element: JetElement?, name: String?): String? = null

                    override fun getIconFlags(): Int = 0

                    override fun getIcon(element: PsiElement?): Icon? =
                            super.getIcon((element as? JetElement)?.getRepresentativeElement())
                },
                "Select target code block",
                {
                    onSelectionComplete(parent, elements, it)
                    true
                }
        ).showInBestPositionFor(editor)
    }

    fun selectMultipleExpressions() {
        val startOffset = editor.getSelectionModel().getSelectionStart()
        val endOffset = editor.getSelectionModel().getSelectionEnd()

        val elements = CodeInsightUtils.findStatements(file, startOffset, endOffset)
        if (elements.isEmpty()) {
            noExpressionError()
            return
        }

        selectTargetContainer(elements.toList())
    }

    fun selectSingleExpression() {
        JetRefactoringUtil.selectExpression(editor, file, false) { expr ->
            if (expr != null) {
                selectTargetContainer(listOf(expr))
            }
            else {
                if (!editor.getSelectionModel().hasSelection()) {
                    editor.getSelectionModel().selectLineAtCaret()
                }
                selectMultipleExpressions()
            }
        }
    }

    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE)
    selectSingleExpression()
}
