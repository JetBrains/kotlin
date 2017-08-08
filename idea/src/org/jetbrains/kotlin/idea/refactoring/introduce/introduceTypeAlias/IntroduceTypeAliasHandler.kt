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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils.ElementKind.TYPE_CONSTRUCTOR
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils.ElementKind.TYPE_ELEMENT
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSupportProvider
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractIntroduceAction
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.processDuplicates
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.ui.KotlinIntroduceTypeAliasDialog
import org.jetbrains.kotlin.idea.refactoring.introduce.selectElementsWithTargetSibling
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHint
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

open class KotlinIntroduceTypeAliasHandler : RefactoringActionHandler {
    companion object {
        @JvmField
        val REFACTORING_NAME = "Introduce Type Alias"

        val INSTANCE = KotlinIntroduceTypeAliasHandler()
    }

    private fun selectElements(editor: Editor, file: KtFile, continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit) {
        selectElementsWithTargetSibling(
                REFACTORING_NAME,
                editor,
                file,
                "Select target code block",
                listOf(TYPE_ELEMENT, TYPE_CONSTRUCTOR),
                { _, parent -> listOf(parent.containingFile) },
                continuation
        )
    }

    private fun runRefactoring(descriptor: IntroduceTypeAliasDescriptor, project: Project, editor: Editor) {
        val typeAlias = project.executeWriteCommand<KtTypeAlias>(REFACTORING_NAME) { descriptor.generateTypeAlias() }

        val duplicateReplacers = findDuplicates(typeAlias)
        if (duplicateReplacers.isNotEmpty()) {
            processDuplicates(duplicateReplacers, project, editor)
        }
    }

    open fun doInvoke(
            project: Project,
            editor: Editor,
            elements: List<PsiElement>,
            targetSibling: PsiElement,
            descriptorSubstitutor: ((IntroduceTypeAliasDescriptor) -> IntroduceTypeAliasDescriptor)? = null
    ) {
        val elementToExtract = elements.singleOrNull()

        val errorMessage = when (elementToExtract) {
            is KtSimpleNameExpression -> {
                if (!(isTypeConstructorReference(elementToExtract) || isDoubleColonReceiver(elementToExtract))) "Type reference is expected" else null
            }
            !is KtTypeElement -> "No type to refactor"
            else -> null
        }
        if (errorMessage != null) return showErrorHint(project, editor, errorMessage, REFACTORING_NAME)

        val introduceData = when (elementToExtract) {
            is KtTypeElement -> IntroduceTypeAliasData(elementToExtract, targetSibling)
            else -> IntroduceTypeAliasData(elementToExtract!!.getStrictParentOfType<KtTypeElement>() ?: elementToExtract as KtElement, targetSibling, true)
        }
        val analysisResult = introduceData.analyze()
        when (analysisResult) {
            is IntroduceTypeAliasAnalysisResult.Error -> {
                return showErrorHint(project, editor, analysisResult.message, REFACTORING_NAME)
            }

            is IntroduceTypeAliasAnalysisResult.Success -> {
                val originalDescriptor = analysisResult.descriptor
                if (ApplicationManager.getApplication().isUnitTestMode) {
                    val (descriptor, conflicts) = descriptorSubstitutor!!(originalDescriptor).validate()
                    project.checkConflictsInteractively(conflicts) { runRefactoring(descriptor, project, editor) }
                }
                else {
                    KotlinIntroduceTypeAliasDialog(project, originalDescriptor) { runRefactoring(it.currentDescriptor, project, editor) }.show()
                }
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is KtFile) return

        val offset = if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart else editor.caretModel.offset

        val refExpression = file.findElementAt(offset)?.getNonStrictParentOfType<KtSimpleNameExpression>()
        if (refExpression != null && isDoubleColonReceiver(refExpression)) {
            return doInvoke(project, editor, listOf(refExpression), refExpression.getOutermostParentContainedIn(file)!!)
        }

        selectElements(editor, file) { elements, targetSibling -> doInvoke(project, editor, elements, targetSibling) }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        throw AssertionError("$REFACTORING_NAME can only be invoked from editor")
    }
}

class IntroduceTypeAliasAction : AbstractIntroduceAction() {
    override fun getRefactoringHandler(provider: RefactoringSupportProvider): RefactoringActionHandler? {
        return if (provider is KotlinRefactoringSupportProvider) KotlinIntroduceTypeAliasHandler.INSTANCE else null
    }

    override fun isAvailableOnElementInEditorAndFile(element: PsiElement, editor: Editor, file: PsiFile, context: DataContext): Boolean {
        return super.isAvailableOnElementInEditorAndFile(element, editor, file, context) &&
               (ModuleUtil.findModuleForPsiElement(file)?.languageVersionSettings?.supportsFeature(LanguageFeature.TypeAliases) ?: false)
    }
}
