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

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui.KotlinExtractFunctionDialog
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.selectElementsWithTargetSibling
import org.jetbrains.kotlin.idea.refactoring.introduce.validateExpressionElements
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFile

class ExtractKotlinFunctionHandler(
    private val allContainersEnabled: Boolean = false,
    private val helper: ExtractionEngineHelper = InteractiveExtractionHelper
) : RefactoringActionHandler {

    object InteractiveExtractionHelper : ExtractionEngineHelper(EXTRACT_FUNCTION) {
        override fun configureAndRun(
            project: Project,
            editor: Editor,
            descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
            onFinish: (ExtractionResult) -> Unit
        ) {
            KotlinExtractFunctionDialog(descriptorWithConflicts.descriptor.extractionData.project, descriptorWithConflicts) {
                doRefactor(it.currentConfiguration, onFinish)
            }.show()
        }
    }

    fun doInvoke(
        editor: Editor,
        file: KtFile,
        elements: List<PsiElement>,
        targetSibling: PsiElement
    ) {
        val adjustedElements = (elements.singleOrNull() as? KtBlockExpression)?.statements ?: elements
        val extractionData = ExtractionData(file, adjustedElements.toRange(false), targetSibling)
        ExtractionEngine(helper).run(editor, extractionData) {
            processDuplicates(it.duplicateReplacers, file.project, editor)
        }
    }

    fun selectElements(editor: Editor, file: KtFile, continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit) {
        selectElementsWithTargetSibling(
            EXTRACT_FUNCTION,
            editor,
            file,
            "Select target code block",
            listOf(CodeInsightUtils.ElementKind.EXPRESSION),
            ::validateExpressionElements,
            { elements, parent -> parent.getExtractionContainers(elements.size == 1, allContainersEnabled) },
            continuation
        )
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is KtFile) return
        selectElements(editor, file) { elements, targetSibling -> doInvoke(editor, file, elements, targetSibling) }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        throw AssertionError("Extract Function can only be invoked from editor")
    }
}

val EXTRACT_FUNCTION: String = KotlinRefactoringBundle.message("extract.function")
