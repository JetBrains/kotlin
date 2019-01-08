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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier

class ConvertObjectLiteralToClassIntention : SelfTargetingRangeIntention<KtObjectLiteralExpression>(
        KtObjectLiteralExpression::class.java,
        "Convert object literal to class"
) {
    override fun applicabilityRange(element: KtObjectLiteralExpression) = element.objectDeclaration.getObjectKeyword()?.textRange

    override fun startInWriteAction() = false

    private fun doApply(editor: Editor, element: KtObjectLiteralExpression, targetParent: KtElement) {
        val project = element.project

        val scope = element.getResolutionScope()

        val validator: (String) -> Boolean = { scope.findClassifier(Name.identifier(it), NoLookupLocation.FROM_IDE) == null }
        val classNames = element.objectDeclaration.superTypeListEntries
            .mapNotNull { it.typeReference?.typeElement?.let { KotlinNameSuggester.suggestTypeAliasNameByPsi(it, validator) } }
            .takeIf { it.isNotEmpty() }
            ?: listOf(KotlinNameSuggester.suggestNameByName("O", validator))

        val className = classNames.first()
        val psiFactory = KtPsiFactory(element)

        val targetSibling = element.parentsWithSelf.first { it.parent == targetParent }

        val objectDeclaration = element.objectDeclaration

        val containingClass = element.containingClass()
        val hasMemberReference = containingClass?.getBody()?.allChildren?.any {
            (it is KtProperty || it is KtNamedFunction) &&
            ReferencesSearch.search(it, element.useScope).findFirst() != null
        } ?: false

        val newClass = psiFactory.createClass("class $className")
        objectDeclaration.getSuperTypeList()?.let {
            newClass.add(psiFactory.createColon())
            newClass.add(it)
        }
        objectDeclaration.getBody()?.let {
            newClass.add(it)
        }

        project.executeWriteCommand(text) {
            ExtractionEngine(
                    object : ExtractionEngineHelper(text) {
                        override fun configureAndRun(
                                project: Project,
                                editor: Editor,
                                descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
                                onFinish: (ExtractionResult) -> Unit
                        ) {
                            val descriptor = descriptorWithConflicts.descriptor.copy(suggestedNames = listOf(className))
                            doRefactor(
                                    ExtractionGeneratorConfiguration(descriptor, ExtractionGeneratorOptions.DEFAULT),
                                    onFinish
                            )
                        }
                    }
            ).run(editor, ExtractionData(element.containingKtFile, element.toRange(), targetSibling)) { extractionResult ->
                val functionDeclaration = extractionResult.declaration as KtFunction
                if (functionDeclaration.valueParameters.isNotEmpty()) {
                    val valKeyword = psiFactory.createValKeyword()
                    newClass
                            .createPrimaryConstructorParameterListIfAbsent()
                            .replaced(functionDeclaration.valueParameterList!!)
                            .parameters
                            .forEach {
                                it.addAfter(valKeyword, null)
                                it.addModifier(KtTokens.PRIVATE_KEYWORD)
                            }
                }

                val introducedClass = functionDeclaration.replaced(newClass).apply {
                    if (hasMemberReference && containingClass == (parent.parent as? KtClass)) addModifier(KtTokens.INNER_KEYWORD)
                    primaryConstructor?.let { CodeStyleManager.getInstance(project).reformat(it) }
                }.let { CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(it) }

                val file = introducedClass.containingFile

                val template = TemplateBuilderImpl(file).let { builder ->
                    builder.replaceElement(introducedClass.nameIdentifier!!, NEW_CLASS_NAME, ChooseStringExpression(classNames), true)
                    for (psiReference in ReferencesSearch.search(introducedClass, LocalSearchScope(file), false)) {
                        builder.replaceElement(psiReference.element, USAGE_VARIABLE_NAME, NEW_CLASS_NAME, false)
                    }
                    builder.buildInlineTemplate()
                }

                editor.caretModel.moveToOffset(file.startOffset)
                TemplateManager.getInstance(project).startTemplate(editor, template)
            }
        }
    }

    override fun applyTo(element: KtObjectLiteralExpression, editor: Editor?) {
        if (editor == null) return

        val containers = element.getExtractionContainers(strict = true, includeAll = true)

        if (ApplicationManager.getApplication().isUnitTestMode) {
            val targetComment = element.containingKtFile.findDescendantOfType<PsiComment>()?.takeIf {
                it.text == "// TARGET_BLOCK:"
            }
            val target = containers.firstOrNull { it == targetComment?.parent } ?: containers.last()
            return doApply(editor, element, target)
        }

        chooseContainerElementIfNecessary(
                containers,
                editor,
                if (containers.first() is KtFile) "Select target file" else "Select target code block / file",
                true,
                { it },
                { doApply(editor, element, it) }
        )
    }
}

private const val NEW_CLASS_NAME = "NEW_CLASS_NAME"

private const val USAGE_VARIABLE_NAME = "OTHER_VAR"
