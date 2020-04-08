/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.chooseApplicableComponentFunctions
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.suggestNamesForComponent
import org.jetbrains.kotlin.idea.resolve.ideService
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class IterateExpressionIntention : SelfTargetingIntention<KtExpression>(
    KtExpression::class.java,
    KotlinBundle.lazyMessage("iterate.over.collection")
),
    HighPriorityAction {
    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        if (element.parent !is KtBlockExpression) return false
        val range = element.textRange
        if (caretOffset != range.startOffset && caretOffset != range.endOffset) return false
        val data = data(element) ?: return false
        setTextGetter(
            KotlinBundle.lazyMessage(
                "iterate.over.0",
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(data.collectionType)
            )
        )

        return true
    }

    private data class Data(val collectionType: KotlinType, val elementType: KotlinType)

    private fun data(expression: KtExpression): Data? {
        val resolutionFacade = expression.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(expression, BodyResolveMode.PARTIAL)
        val type = bindingContext.getType(expression) ?: return null
        if (KotlinBuiltIns.isNothing(type)) return null
        val scope = expression.getResolutionScope(bindingContext, resolutionFacade)
        val detector = resolutionFacade.ideService<IterableTypesDetection>().createDetector(scope)
        val elementType = detector.elementType(type)?.type ?: return null
        return Data(type, elementType)
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtExpression, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")
        val elementType = data(element)!!.elementType
        val nameValidator = NewDeclarationNameValidator(element, element.siblings(), NewDeclarationNameValidator.Target.VARIABLES)
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)

        val project = element.project
        val psiFactory = KtPsiFactory(project)

        val receiverExpression = psiFactory.createExpressionByPattern("$0.iterator().next()", element)
        chooseApplicableComponentFunctions(element, editor, elementType, receiverExpression) { componentFunctions ->
            project.executeWriteCommand(text) {
                val names = if (componentFunctions.isNotEmpty()) {
                    val collectingValidator = CollectingNameValidator(filter = nameValidator)
                    componentFunctions.map { suggestNamesForComponent(it, project, collectingValidator) }
                } else {
                    listOf(KotlinNameSuggester.suggestIterationVariableNames(element, elementType, bindingContext, nameValidator, "e"))
                }

                val paramPattern = (names.asSequence().singleOrNull()?.first()
                    ?: psiFactory.createDestructuringParameter(names.indices.joinToString(prefix = "(", postfix = ")") { "p$it" }))
                var forExpression = psiFactory.createExpressionByPattern("for($0 in $1) {\nx\n}", paramPattern, element) as KtForExpression
                forExpression = element.replaced(forExpression)

                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(forExpression)?.let { expression ->
                    val bodyPlaceholder = (expression.body as KtBlockExpression).statements.single()
                    val parameters = expression.destructuringDeclaration?.entries ?: listOf(expression.loopParameter!!)

                    val templateBuilder = TemplateBuilderImpl(expression)
                    for ((parameter, parameterNames) in (parameters zip names)) {
                        templateBuilder.replaceElement(parameter, ChooseStringExpression(parameterNames))
                    }
                    templateBuilder.replaceElement(bodyPlaceholder, ConstantNode(""), false)
                    templateBuilder.setEndVariableAfter(bodyPlaceholder)

                    templateBuilder.run(editor, true)
                }
            }
        }
    }
}