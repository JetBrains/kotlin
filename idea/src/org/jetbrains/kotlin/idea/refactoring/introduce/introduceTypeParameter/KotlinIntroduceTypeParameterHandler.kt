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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeParameter

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter.CreateTypeParameterData
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter.CreateTypeParameterFromUsageFix
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractIntroduceAction
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.processDuplicates
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.KotlinIntroduceTypeAliasHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.isObjectOrNonInnerClass
import org.jetbrains.kotlin.idea.refactoring.introduce.selectElementsWithTargetParent
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHint
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.utils.keysToMap

object KotlinIntroduceTypeParameterHandler : RefactoringActionHandler {
    @JvmField
    val REFACTORING_NAME = "Introduce Type Parameter"

    fun selectElements(editor: Editor, file: KtFile, continuation: (elements: List<PsiElement>, targetParent: PsiElement) -> Unit) {
        selectElementsWithTargetParent(
                REFACTORING_NAME,
                editor,
                file,
                "Introduce type parameter to declaration",
                listOf(CodeInsightUtils.ElementKind.TYPE_ELEMENT),
                { elements, parent ->
                    val stopAt = parent.parents.firstOrNull(::isObjectOrNonInnerClass)?.parent
                    (if (stopAt != null) parent.parents.takeWhile { it != stopAt } else parent.parents)
                            .filter {
                                ((it is KtClass && !it.isInterface() && it !is KtEnumEntry) ||
                                 it is KtNamedFunction ||
                                 (it is KtProperty && !it.isLocal) ||
                                 it is KtTypeAlias) &&
                                it is KtTypeParameterListOwner &&
                                it.nameIdentifier != null
                            }
                            .toList()
                },
                continuation
        )
    }

    fun doInvoke(project: Project, editor: Editor, elements: List<PsiElement>, targetParent: PsiElement) {
        val targetOwner = targetParent as KtTypeParameterListOwner
        val typeToExtract = elements.singleOrNull() as? KtTypeElement
                            ?: return showErrorHint(project, editor, "No type to refactor", REFACTORING_NAME)

        val scope = targetOwner.getResolutionScope()
        val initialName = KotlinNameSuggester.suggestNamesForTypeParameters(1) {
            scope.findClassifier(Name.identifier(it), NoLookupLocation.FROM_IDE) == null
        }.single()

        val context = typeToExtract.analyze(BodyResolveMode.PARTIAL)
        val upperBoundType = typeToExtract.getAbbreviatedTypeOrType(context)

        val duplicateRanges = typeToExtract
                .toRange()
                .match(targetParent, KotlinPsiUnifier.DEFAULT)
                .filterNot {
                    val textRange = it.range.getTextRange()
                    typeToExtract.textRange.intersects(textRange) || targetOwner.typeParameterList?.textRange?.intersects(textRange) ?: false
                }
                .mapNotNull { it.range.elements.toRange() }

        project.executeCommand(REFACTORING_NAME) {
            val newTypeParameter = CreateTypeParameterFromUsageFix(
                    typeToExtract,
                    CreateTypeParameterData(initialName, targetOwner, upperBoundType)
            ).doInvoke() ?: return@executeCommand

            val newName = newTypeParameter.name ?: return@executeCommand
            val parameterRefElement = KtPsiFactory(project).createType(newName).typeElement!!

            runWriteAction { typeToExtract.replace(parameterRefElement) }

            processDuplicates(
                    duplicateRanges.keysToMap {
                        fun() {
                            it.elements.singleOrNull()?.replace(parameterRefElement)
                        }
                    },
                    project,
                    editor
            )
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