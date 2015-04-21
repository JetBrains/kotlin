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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*
import com.intellij.openapi.editor.*
import org.jetbrains.kotlin.idea.util.psi.patternMatching.*
import com.intellij.openapi.application.*
import com.intellij.refactoring.*
import org.jetbrains.kotlin.idea.util.application.*
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.refactoring.introduce.*
import com.intellij.ui.awt.*
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.ui.*
import javax.swing.event.*
import com.intellij.openapi.project.*
import org.jetbrains.kotlin.idea.core.refactoring.checkConflictsInteractively

public abstract class ExtractionEngineHelper(val operationName: String) {
    open fun adjustExtractionData(data: ExtractionData): ExtractionData = data

    fun doRefactor(config: ExtractionGeneratorConfiguration, onFinish: (ExtractionResult) -> Unit = {}) {
        val project = config.descriptor.extractionData.project
        onFinish(project.executeWriteCommand<ExtractionResult>(operationName) { config.generateDeclaration() })
    }

    abstract fun configureAndRun(
            project: Project,
            editor: Editor,
            descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
            onFinish: (ExtractionResult) -> Unit = {}
    )
}

public class ExtractionEngine(
        val helper: ExtractionEngineHelper
) {
    fun run(editor: Editor,
            extractionData: ExtractionData,
            onFinish: (ExtractionResult) -> Unit = {}
    ) {
        val project = extractionData.project

        val analysisResult = helper.adjustExtractionData(extractionData).performAnalysis()

        if (ApplicationManager.getApplication()!!.isUnitTestMode() && analysisResult.status != AnalysisResult.Status.SUCCESS) {
            throw BaseRefactoringProcessor.ConflictsInTestsException(analysisResult.messages.map { it.renderMessage() })
        }

        fun validateAndRefactor() {
            val validationResult = analysisResult.descriptor!!.validate()
            project.checkConflictsInteractively(validationResult.conflicts) {
                helper.configureAndRun(project, editor, validationResult, onFinish)
            }
        }

        val message = analysisResult.messages.map { it.renderMessage() }.joinToString("\n")
        when (analysisResult.status) {
            AnalysisResult.Status.CRITICAL_ERROR -> {
                showErrorHint(project, editor, message, helper.operationName)
            }

            AnalysisResult.Status.NON_CRITICAL_ERROR -> {
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
                        .show(anchorPoint, Balloon.Position.below)
            }

            AnalysisResult.Status.SUCCESS -> validateAndRefactor()
        }
    }
}
