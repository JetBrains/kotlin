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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceProperty

import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.refactoring.introduce.*
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.util.psi.patternMatching.*
import com.intellij.openapi.application.*
import com.intellij.refactoring.RefactoringActionHandler
import org.jetbrains.kotlin.idea.refactoring.getExtractionContainers
import java.util.*

public class KotlinIntroducePropertyHandler(
        val helper: ExtractionEngineHelper = KotlinIntroducePropertyHandler.InteractiveExtractionHelper
): RefactoringActionHandler {
    object InteractiveExtractionHelper : ExtractionEngineHelper(INTRODUCE_PROPERTY) {
        override fun configureAndRun(
                project: Project,
                editor: Editor,
                descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
                onFinish: (ExtractionResult) -> Unit
        ) {
            val descriptor = descriptorWithConflicts.descriptor
            val target = propertyTargets.filter { it.isAvailable(descriptor) }.firstOrNull()
            if (target != null) {
                val options = ExtractionGeneratorOptions.DEFAULT.copy(target = target, delayInitialOccurrenceReplacement = true)
                doRefactor(ExtractionGeneratorConfiguration(descriptor, options), onFinish)
            }
            else {
                showErrorHint(project, editor, "Can't introduce property for this expression", INTRODUCE_PROPERTY)
            }
        }
    }

    public fun selectElements(editor: Editor, file: KtFile, continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit) {
        selectElementsWithTargetSibling(
                INTRODUCE_PROPERTY,
                editor,
                file,
                { elements, parent ->
                    parent.getExtractionContainers(strict = true, includeAll = true).filter { it is KtClassBody || (it is KtFile && !it.isScript) }
                },
                continuation
        )
    }

    public fun doInvoke(project: Project, editor: Editor, file: KtFile, elements: List<PsiElement>, targetSibling: PsiElement) {
        val adjustedElements = (elements.singleOrNull() as? KtBlockExpression)?.statements ?: elements
        if (adjustedElements.isNotEmpty()) {
            val options = ExtractionOptions(extractAsProperty = true)
            val extractionData = ExtractionData(file, adjustedElements.toRange(), targetSibling, null, options)
            ExtractionEngine(helper).run(editor, extractionData) {
                val property = it.declaration as KtProperty
                val descriptor = it.config.descriptor

                editor.caretModel.moveToOffset(property.textOffset)
                editor.selectionModel.removeSelection()
                if (editor.settings.isVariableInplaceRenameEnabled && !ApplicationManager.getApplication().isUnitTestMode) {
                    with(PsiDocumentManager.getInstance(project)) {
                        commitDocument(editor.document)
                        doPostponedOperationsAndUnblockDocument(editor.document)
                    }

                    val introducer = KotlinInplacePropertyIntroducer(
                            property = property,
                            editor = editor,
                            project = project,
                            title = INTRODUCE_PROPERTY,
                            doNotChangeVar = false,
                            exprType = descriptor.returnType,
                            extractionResult = it,
                            availableTargets = propertyTargets.filter { it.isAvailable(descriptor) }
                    )
                    introducer.performInplaceRefactoring(LinkedHashSet(descriptor.suggestedNames))
                }
                else {
                    processDuplicatesSilently(it.duplicateReplacers, project)
                }
            }
        }
        else {
            showErrorHintByKey(project, editor, "cannot.refactor.no.expression", INTRODUCE_PROPERTY)
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is KtFile) return
        selectElements(editor, file) { elements, targetSibling -> doInvoke(project, editor, file, elements, targetSibling) }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        throw AssertionError("$INTRODUCE_PROPERTY can only be invoked from editor")
    }
}

val INTRODUCE_PROPERTY: String = KotlinRefactoringBundle.message("introduce.property")