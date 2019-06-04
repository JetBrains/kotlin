/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceProperty

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.selectElementsWithTargetSibling
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHint
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHintByKey
import org.jetbrains.kotlin.idea.refactoring.introduce.validateExpressionElements
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import java.util.*

class KotlinIntroduceLateinitPropertyHandler(
    val helper: ExtractionEngineHelper = InteractiveExtractionHelper
) : RefactoringActionHandler {
    object InteractiveExtractionHelper : ExtractionEngineHelper(INTRODUCE_LATEINIT_PROPERTY) {
        private fun getExtractionTarget(descriptor: ExtractableCodeDescriptor) =
            ExtractionTarget.LATEINIT_PROPERTY.takeIf { it.isAvailable(descriptor) }

        override fun validate(descriptor: ExtractableCodeDescriptor) =
            descriptor.validate(getExtractionTarget(descriptor) ?: ExtractionTarget.FUNCTION)

        override fun configureAndRun(
            project: Project,
            editor: Editor,
            descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
            onFinish: (ExtractionResult) -> Unit
        ) {
            val descriptor = descriptorWithConflicts.descriptor
            val target = getExtractionTarget(descriptor)
            if (target != null) {
                val options = ExtractionGeneratorOptions.DEFAULT.copy(target = target, delayInitialOccurrenceReplacement = true)
                doRefactor(ExtractionGeneratorConfiguration(descriptor, options), onFinish)
            } else {
                showErrorHint(project, editor, "Can't introduce lateinit property for this expression", INTRODUCE_LATEINIT_PROPERTY)
            }
        }
    }

    fun selectElements(editor: Editor, file: KtFile, continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit) {
        selectElementsWithTargetSibling(
            INTRODUCE_LATEINIT_PROPERTY,
            editor,
            file,
            "Select target code block",
            listOf(CodeInsightUtils.ElementKind.EXPRESSION),
            ::validateExpressionElements,
            { _, parent ->
                parent.getExtractionContainers(strict = true, includeAll = true)
                    .filter { it is KtClassBody || (it is KtFile && !it.isScript()) }
            },
            continuation
        )
    }

    fun doInvoke(project: Project, editor: Editor, file: KtFile, elements: List<PsiElement>, targetSibling: PsiElement) {
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

                    val introducer = KotlinInplaceLateinitPropertyIntroducer(
                        property = property,
                        editor = editor,
                        project = project,
                        title = INTRODUCE_LATEINIT_PROPERTY,
                        doNotChangeVar = false,
                        exprType = descriptor.returnType,
                        extractionResult = it,
                        availableTargets = listOf(ExtractionTarget.LATEINIT_PROPERTY).filter { target -> target.isAvailable(descriptor) }
                    )
                    introducer.performInplaceRefactoring(LinkedHashSet(descriptor.suggestedNames))
                } else {
                    processDuplicatesSilently(it.duplicateReplacers, project)
                }
            }
        } else {
            showErrorHintByKey(project, editor, "cannot.refactor.no.expression", INTRODUCE_LATEINIT_PROPERTY)
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is KtFile) return
        selectElements(editor, file) { elements, targetSibling -> doInvoke(project, editor, file, elements, targetSibling) }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        throw AssertionError("$INTRODUCE_LATEINIT_PROPERTY can only be invoked from editor")
    }
}

val INTRODUCE_LATEINIT_PROPERTY: String = KotlinRefactoringBundle.message("introduce.lateinit-property")