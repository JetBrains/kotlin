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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction

import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiFile
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui.KotlinExtractFunctionDialog
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetBlockExpression
import kotlin.test.fail
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.Status
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.MessageType
import javax.swing.event.HyperlinkEvent
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.ui.awt.RelativePoint
import com.intellij.openapi.ui.popup.Balloon.Position
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.idea.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.*

public open class ExtractKotlinFunctionHandlerHelper {
    open fun adjustExtractionData(data: ExtractionData): ExtractionData = data
    open fun adjustGeneratorOptions(options: ExtractionGeneratorOptions): ExtractionGeneratorOptions = options
    open fun adjustDescriptor(descriptor: ExtractableCodeDescriptor): ExtractableCodeDescriptor = descriptor

    class object {
        public val DEFAULT: ExtractKotlinFunctionHandlerHelper = ExtractKotlinFunctionHandlerHelper()
    }
}

public class ExtractKotlinFunctionHandler(
        public val allContainersEnabled: Boolean = false,
        private val helper: ExtractKotlinFunctionHandlerHelper = ExtractKotlinFunctionHandlerHelper.DEFAULT) : RefactoringActionHandler {
    private fun adjustElements(elements: List<PsiElement>): List<PsiElement> {
        if (elements.size() != 1) return elements

        val e = elements.first()
        if (e is JetBlockExpression && e.getParent() is JetFunctionLiteral) return e.getStatements()

        return elements
    }

    fun doInvoke(
            editor: Editor,
            file: JetFile,
            elements: List<PsiElement>,
            targetSibling: PsiElement
    ) {
        val project = file.getProject()

        val analysisResult = helper.adjustExtractionData(
                ExtractionData(file, adjustElements(elements).toRange(false), targetSibling)
        ).performAnalysis()

        if (ApplicationManager.getApplication()!!.isUnitTestMode() && analysisResult.status != Status.SUCCESS) {
            throw ConflictsInTestsException(analysisResult.messages.map { it.renderMessage() })
        }

        fun doRefactor(descriptor: ExtractableCodeDescriptor, generatorOptions: ExtractionGeneratorOptions) {
            val adjustedDescriptor = helper.adjustDescriptor(descriptor)
            val adjustedGeneratorOptions = helper.adjustGeneratorOptions(generatorOptions)
            val result = project.executeWriteCommand<ExtractionResult>(EXTRACT_FUNCTION) { adjustedDescriptor.generateDeclaration(adjustedGeneratorOptions) }
            processDuplicates(result.duplicateReplacers, project, editor)
        }

        fun validateAndRefactor() {
            val validationResult = analysisResult.descriptor!!.validate()
            project.checkConflictsInteractively(validationResult.conflicts) {
                if (ApplicationManager.getApplication()!!.isUnitTestMode()) {
                    doRefactor(validationResult.descriptor, ExtractionGeneratorOptions.DEFAULT)
                }
                else {
                    KotlinExtractFunctionDialog(project, validationResult) {
                        doRefactor(it.getCurrentDescriptor(), it.getGeneratorOptions())
                    }.show()
                }
            }
        }

        val message = analysisResult.messages.map { it.renderMessage() }.joinToString("\n")
        when (analysisResult.status) {
            Status.CRITICAL_ERROR -> {
                showErrorHint(project, editor, message, EXTRACT_FUNCTION)
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

    fun selectElements(editor: Editor, file: PsiFile, continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit) {
        selectElements(
                EXTRACT_FUNCTION,
                editor,
                file,
                {(elements, parent) -> parent.getExtractionContainers(elements.size() == 1, allContainersEnabled) },
                continuation
        )
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is JetFile) return
        selectElements(editor, file) { (elements, targetSibling) -> doInvoke(editor, file, elements, targetSibling) }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        fail("Extract Function can only be invoked from editor")
    }
}

private val EXTRACT_FUNCTION: String = JetRefactoringBundle.message("extract.function")
