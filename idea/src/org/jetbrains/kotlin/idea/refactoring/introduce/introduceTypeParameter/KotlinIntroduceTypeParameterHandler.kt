/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeParameter

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.usageView.UsageViewTypeLocation
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter.CreateTypeParameterByUnresolvedRefActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter.CreateTypeParameterFromUsageFix
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter.getPossibleTypeParameterContainers
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractIntroduceAction
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.processDuplicates
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.KotlinIntroduceTypeAliasHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.selectElementsWithTargetParent
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHint
import org.jetbrains.kotlin.idea.refactoring.rename.VariableInplaceRenameHandlerWithFinishHook
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.utils.keysToMap

object KotlinIntroduceTypeParameterHandler : RefactoringActionHandler {
    @JvmField
    val REFACTORING_NAME = KotlinBundle.message("introduce.type.parameter")

    fun selectElements(editor: Editor, file: KtFile, continuation: (elements: List<PsiElement>, targetParent: PsiElement) -> Unit) {
        selectElementsWithTargetParent(
            REFACTORING_NAME,
            editor,
            file,
            KotlinBundle.message("introduce.type.parameter.to.declaration"),
            listOf(CodeInsightUtils.ElementKind.TYPE_ELEMENT),
            { null },
            { _, parent -> getPossibleTypeParameterContainers(parent) },
            continuation
        )
    }

    fun doInvoke(project: Project, editor: Editor, elements: List<PsiElement>, targetParent: PsiElement) {
        val targetOwner = targetParent as KtTypeParameterListOwner
        val typeElementToExtract =
            elements.singleOrNull() as? KtTypeElement ?: return showErrorHint(
                project,
                editor,
                KotlinBundle.message("error.text.no.type.to.refactor"),
                REFACTORING_NAME
            )

        val typeElementToExtractPointer = typeElementToExtract.createSmartPointer()

        val scope = targetOwner.getResolutionScope()
        val suggestedNames = KotlinNameSuggester.suggestNamesForTypeParameters(
            1,
            CollectingNameValidator(targetOwner.typeParameters.mapNotNull { it.name }) {
                scope.findClassifier(Name.identifier(it), NoLookupLocation.FROM_IDE) == null
            }
        )
        val defaultName = suggestedNames.single()

        val context = typeElementToExtract.analyze(BodyResolveMode.PARTIAL)
        val originalType = typeElementToExtract.getAbbreviatedTypeOrType(context)

        val createTypeParameterData =
            CreateTypeParameterByUnresolvedRefActionFactory.extractFixData(typeElementToExtract, defaultName)?.let {
                it.copy(typeParameters = listOf(it.typeParameters.single().copy(upperBoundType = originalType)), declaration = targetOwner)
            } ?: return showErrorHint(
                project,
                editor,
                KotlinBundle.message("error.text.refactoring.is.not.applicable.in.the.current.context"),
                REFACTORING_NAME
            )

        project.executeCommand(REFACTORING_NAME) {
            val newTypeParameter =
                CreateTypeParameterFromUsageFix(typeElementToExtract, createTypeParameterData, false).doInvoke().singleOrNull()
                    ?: return@executeCommand
            val newTypeParameterPointer = newTypeParameter.createSmartPointer()

            val postRename = postRename@{
                val restoredTypeParameter = newTypeParameterPointer.element ?: return@postRename
                val restoredOwner = restoredTypeParameter.getStrictParentOfType<KtTypeParameterListOwner>() ?: return@postRename
                val restoredOriginalTypeElement = typeElementToExtractPointer.element ?: return@postRename

                val parameterRefElement = KtPsiFactory(project).createType(restoredTypeParameter.name ?: "_").typeElement!!

                val duplicateRanges = restoredOriginalTypeElement
                    .toRange()
                    .match(restoredOwner, KotlinPsiUnifier.DEFAULT)
                    .asSequence()
                    .filterNot {
                        val textRange = it.range.getTextRange()
                        restoredOriginalTypeElement.textRange.intersects(textRange)
                                || restoredOwner.typeParameterList?.textRange?.intersects(textRange) ?: false
                    }
                    .map { it.range.elements.toRange() }
                    .toList()

                runWriteAction {
                    restoredOriginalTypeElement.replace(parameterRefElement)
                }

                processDuplicates(
                    duplicateRanges.keysToMap {
                        {
                            it.elements.singleOrNull()?.replace(parameterRefElement)
                            Unit
                        }
                    },
                    project,
                    editor,
                    ElementDescriptionUtil.getElementDescription(
                        restoredOwner,
                        UsageViewTypeLocation.INSTANCE
                    ) + " '${restoredOwner.name}'",
                    KotlinBundle.message("description.a.reference.to.extracted.type.parameter")
                )

                restoredTypeParameter.extendsBound?.let {
                    editor.selectionModel.setSelection(it.startOffset, it.endOffset)
                    editor.caretModel.moveToOffset(it.startOffset)
                }
            }

            if (!ApplicationManager.getApplication().isUnitTestMode) {
                val dataContext = SimpleDataContext.getSimpleContext(
                    CommonDataKeys.PSI_ELEMENT.name, newTypeParameter,
                    (editor as? EditorEx)?.dataContext
                )
                editor.selectionModel.removeSelection()
                editor.caretModel.moveToOffset(newTypeParameter.startOffset)
                VariableInplaceRenameHandlerWithFinishHook(postRename).doRename(newTypeParameter, editor, dataContext)
            } else {
                postRename()
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is KtFile) return
        selectElements(editor, file) { elements, targetParent -> doInvoke(project, editor, elements, targetParent) }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        throw AssertionError("${KotlinIntroduceTypeAliasHandler.REFACTORING_NAME} can only be invoked from editor")
    }
}

class IntroduceTypeParameterAction : AbstractIntroduceAction() {
    override fun getRefactoringHandler(provider: RefactoringSupportProvider) = KotlinIntroduceTypeParameterHandler
}