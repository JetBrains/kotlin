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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.analysis.computeTypeInfoInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.moveInsideParenthesesAndReplaceWith
import org.jetbrains.kotlin.idea.core.refactoring.Pass
import org.jetbrains.kotlin.idea.core.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.intentions.ConvertToBlockBodyIntention
import org.jetbrains.kotlin.idea.intentions.RemoveCurlyBracesFromTemplateIntention
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtil
import org.jetbrains.kotlin.idea.refactoring.introduce.KotlinIntroduceHandlerBase
import org.jetbrains.kotlin.idea.refactoring.introduce.findElementByCopyableDataAndClearIt
import org.jetbrains.kotlin.idea.refactoring.introduce.findExpressionByCopyableDataAndClearIt
import org.jetbrains.kotlin.idea.refactoring.introduce.findExpressionsByCopyableDataAndClearIt
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.ObservableBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.ifEmpty
import org.jetbrains.kotlin.utils.sure
import java.util.*

object KotlinIntroduceVariableHandler : KotlinIntroduceHandlerBase() {
    val INTRODUCE_VARIABLE = KotlinRefactoringBundle.message("introduce.variable")

    private val EXPRESSION_KEY = Key.create<Boolean>("EXPRESSION_KEY")
    private val REPLACE_KEY = Key.create<Boolean>("REPLACE_KEY")
    private val COMMON_PARENT_KEY = Key.create<Boolean>("COMMON_PARENT_KEY")

    private var KtExpression.isOccurrence: Boolean by NotNullableCopyableUserDataProperty(Key.create("OCCURRENCE"), false)

    private class IntroduceVariableContext(
            private val expression: KtExpression,
            private val nameSuggestion: String,
            private val allReplaces: List<KtExpression>,
            private val commonContainer: PsiElement,
            private val commonParent: PsiElement,
            private val replaceOccurrence: Boolean,
            private val noTypeInference: Boolean,
            private val expressionType: KotlinType?,
            private val bindingContext: BindingContext,
            private val resolutionFacade: ResolutionFacade
    ) {
        private val psiFactory = KtPsiFactory(expression)

        var propertyRef: KtProperty? = null
        var reference: KtExpression? = null
        val references = ArrayList<KtExpression>()

        private fun findElementByOffsetAndText(offset: Int, text: String, newContainer: PsiElement): PsiElement? {
            return newContainer.findElementAt(offset)?.parentsWithSelf?.firstOrNull { (it as? KtExpression)?.text == text }
        }

        private fun replaceExpression(replace: KtExpression, addToReferences: Boolean): KtExpression {
            val isActualExpression = expression == replace

            val replacement = psiFactory.createExpression(nameSuggestion)
            var result = if (replace.isFunctionLiteralOutsideParentheses()) {
                val functionLiteralArgument = replace.getStrictParentOfType<KtFunctionLiteralArgument>()!!
                val newCallExpression = functionLiteralArgument.moveInsideParenthesesAndReplaceWith(replacement, bindingContext)
                newCallExpression.valueArguments.last().getArgumentExpression()!!
            }
            else {
                replace.replace(replacement) as KtExpression
            }

            val parent = result.parent
            if (parent is KtBlockStringTemplateEntry) {
                val intention = RemoveCurlyBracesFromTemplateIntention()
                val newEntry = if (intention.isApplicableTo(parent)) intention.applyTo(parent) else parent
                result = newEntry.expression!!
            }

            if (addToReferences) references.addIfNotNull(result)

            if (isActualExpression) reference = result

            return result
        }

        private fun runRefactoring (
                expression: KtExpression,
                commonContainer: PsiElement,
                commonParent: PsiElement,
                allReplaces: List<KtExpression>
        ) {
            val variableText = StringBuilder("val ").apply {
                append(nameSuggestion)
                if (noTypeInference) {
                    val typeToRender = expressionType ?: resolutionFacade.moduleDescriptor.builtIns.anyType
                    append(": ").append(IdeDescriptorRenderers.SOURCE_CODE.renderType(typeToRender))
                }
                append(" = ")

                append(((expression as? KtParenthesizedExpression)?.expression ?: expression).text)
            }.toString()
            var property = psiFactory.createProperty(variableText)

            var anchor = calculateAnchor(commonParent, commonContainer, allReplaces) ?: return
            val needBraces = commonContainer !is KtBlockExpression
            if (!needBraces) {
                property = commonContainer.addBefore(property, anchor) as KtProperty
                commonContainer.addBefore(psiFactory.createNewLine(), anchor)
            }
            else {
                var emptyBody: KtExpression = psiFactory.createEmptyBody()
                val firstChild = emptyBody.firstChild
                emptyBody.addAfter(psiFactory.createNewLine(), firstChild)

                if (replaceOccurrence) {
                    for (replace in allReplaces) {
                        val exprAfterReplace = replaceExpression(replace, false)
                        exprAfterReplace.isOccurrence = true
                        if (anchor == replace) {
                            anchor = exprAfterReplace
                        }
                    }

                    var oldElement: PsiElement = commonContainer
                    if (commonContainer is KtWhenEntry) {
                        val body = commonContainer.expression
                        if (body != null) {
                            oldElement = body
                        }
                    }
                    else if (commonContainer is KtContainerNode) {
                        val children = commonContainer.children
                        for (child in children) {
                            if (child is KtExpression) {
                                oldElement = child
                            }
                        }
                    }
                    //ugly logic to make sure we are working with right actual expression
                    var actualExpression = reference!!
                    var diff = actualExpression.textRange.startOffset - oldElement.textRange.startOffset
                    var actualExpressionText = actualExpression.text
                    val newElement = emptyBody.addAfter(oldElement, firstChild)
                    var elem: PsiElement? = findElementByOffsetAndText(diff, actualExpressionText, newElement)
                    if (elem != null) {
                        reference = elem as KtExpression
                    }
                    emptyBody.addAfter(psiFactory.createNewLine(), firstChild)
                    property = emptyBody.addAfter(property, firstChild) as KtProperty
                    emptyBody.addAfter(psiFactory.createNewLine(), firstChild)
                    actualExpression = reference!!
                    diff = actualExpression.textRange.startOffset - emptyBody.textRange.startOffset
                    actualExpressionText = actualExpression.text
                    emptyBody = anchor!!.replace(emptyBody) as KtBlockExpression
                    elem = findElementByOffsetAndText(diff, actualExpressionText, emptyBody)
                    if (elem != null) {
                        reference = elem as KtExpression
                    }

                    emptyBody.accept(
                            object : KtTreeVisitorVoid() {
                                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                                    if (!expression.isOccurrence) return

                                    expression.isOccurrence = false
                                    references.add(expression)
                                }
                            })
                }
                else {
                    val parent = anchor.parent
                    val copyTo = parent.lastChild
                    val copyFrom = anchor.nextSibling

                    property = emptyBody.addAfter(property, firstChild) as KtProperty
                    emptyBody.addAfter(psiFactory.createNewLine(), firstChild)
                    if (copyFrom != null && copyTo != null) {
                        emptyBody.addRangeAfter(copyFrom, copyTo, property)
                        parent.deleteChildRange(copyFrom, copyTo)
                    }
                    emptyBody = anchor.replace(emptyBody) as KtBlockExpression
                }
                for (child in emptyBody.children) {
                    if (child is KtProperty) {
                        property = child
                    }
                }
                if (commonContainer is KtContainerNode) {
                    if (commonContainer.parent is KtIfExpression) {
                        val next = commonContainer.nextSibling
                        if (next != null) {
                            val nextnext = next.nextSibling
                            if (nextnext != null && nextnext.node.elementType == KtTokens.ELSE_KEYWORD) {
                                if (next is PsiWhiteSpace) {
                                    next.replace(psiFactory.createWhiteSpace())
                                }
                            }
                        }
                    }
                }
            }
            if (!needBraces) {
                for (i in allReplaces.indices) {
                    val replace = allReplaces[i]

                    if (if (i != 0) replaceOccurrence else replace.shouldReplaceOccurrence(bindingContext, commonContainer)) {
                        replaceExpression(replace, true)
                    }
                    else {
                        val sibling = PsiTreeUtil.skipSiblingsBackward(replace, PsiWhiteSpace::class.java)
                        if (sibling == property) {
                            replace.parent.deleteChildRange(property.nextSibling, replace)
                        }
                        else {
                            replace.delete()
                        }
                    }
                }
            }
            propertyRef = property
            if (noTypeInference) {
                ShortenReferences.DEFAULT.process(property)
            }
        }

        fun runRefactoring() {
            if (commonContainer !is KtDeclarationWithBody) return runRefactoring(expression, commonContainer, commonParent, allReplaces)

            commonContainer.bodyExpression.sure { "Original body is not found: " + commonContainer }

            expression.putCopyableUserData(EXPRESSION_KEY, true)
            for (replace in allReplaces) {
                replace.putCopyableUserData(REPLACE_KEY, true)
            }
            commonParent.putCopyableUserData(COMMON_PARENT_KEY, true)

            val newDeclaration = ConvertToBlockBodyIntention.convert(commonContainer)

            val newCommonContainer = (newDeclaration.bodyExpression as KtBlockExpression?)
                    .sure { "New body is not found: " + newDeclaration }

            val newExpression = newCommonContainer.findExpressionByCopyableDataAndClearIt(EXPRESSION_KEY)
            val newCommonParent = newCommonContainer.findElementByCopyableDataAndClearIt(COMMON_PARENT_KEY)
            val newAllReplaces = newCommonContainer.findExpressionsByCopyableDataAndClearIt(REPLACE_KEY)

            runRefactoring(newExpression, newCommonContainer, newCommonParent, newAllReplaces)
        }
    }

    private fun calculateAnchor(commonParent: PsiElement, commonContainer: PsiElement, allReplaces: List<KtExpression>): PsiElement? {
        if (commonParent != commonContainer) return commonParent.parentsWithSelf.firstOrNull { it.parent == commonContainer }

        val startOffset = allReplaces.fold(commonContainer.endOffset) { offset, expr -> Math.min(offset, expr.startOffset) }
        return commonContainer.allChildren.lastOrNull { it.textRange.contains(startOffset) } ?: return null
    }

    private fun KtExpression.findOccurrences(occurrenceContainer: PsiElement): List<KtExpression> {
        return toRange()
                .match(occurrenceContainer, KotlinPsiUnifier.DEFAULT)
                .map {
                    val candidate = it.range.elements.first()
                    when (candidate) {
                        is KtExpression -> candidate
                        is KtStringTemplateEntryWithExpression -> candidate.expression
                        else -> throw AssertionError("Unexpected candidate element: " + candidate.text)
                    } as? KtExpression
                }
                .filterNotNull()
    }

    private fun KtExpression.shouldReplaceOccurrence(bindingContext: BindingContext, container: PsiElement?): Boolean {
        return isUsedAsExpression(bindingContext) || container != parent
    }

    private fun KtElement.getContainer(): KtElement? {
        if (this is KtBlockExpression) return this

        return (parentsWithSelf zip parents).firstOrNull {
            val (place, parent) = it
            when (parent) {
                is KtContainerNode -> !parent.isBadContainerNode(place)
                is KtBlockExpression -> true
                is KtWhenEntry -> place == parent.expression
                is KtDeclarationWithBody -> parent.bodyExpression == place
                else -> false
            }
        }?.second as? KtElement
    }

    private fun KtContainerNode.isBadContainerNode(place: PsiElement): Boolean {
        val parent = parent
        return when (parent) {
            is KtIfExpression -> parent.condition == place
            is KtLoopExpression -> parent.body != place
            is KtArrayAccessExpression -> true
            else -> false
        }
    }

    private fun KtExpression.getOccurrenceContainer(): KtElement? {
        var result: KtElement? = null
        for ((place, parent) in parentsWithSelf zip parents) {
            when {
                parent is KtContainerNode && place !is KtBlockExpression && !parent.isBadContainerNode(place) -> result = parent
                parent is KtClassBody, parent is KtFile -> return result
                parent is KtBlockExpression -> result = parent
                parent is KtWhenEntry && place !is KtBlockExpression -> result = parent
                parent is KtDeclarationWithBody && parent.bodyExpression == place && place !is KtBlockExpression -> result = parent
            }
        }

        return null
    }

    private fun showErrorHint(project: Project, editor: Editor?, message: String) {
        CommonRefactoringUtil.showErrorHint(project, editor, message, INTRODUCE_VARIABLE, HelpID.INTRODUCE_VARIABLE)
    }

    fun doRefactoring(
            project: Project,
            editor: Editor?,
            expression: KtExpression,
            container: KtElement,
            occurrenceContainer: KtElement,
            resolutionFacade: ResolutionFacade,
            bindingContext: BindingContext,
            occurrencesToReplace: List<KtExpression>?,
            onNonInteractiveFinish: ((KtProperty) -> Unit)?
    ) {
        val parent = expression.parent

        when {
            parent is KtQualifiedExpression -> {
                if (parent.receiverExpression != expression) {
                    return showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.expression"))
                }
            }
            expression is KtStatementExpression ->
                return showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.expression"))
            parent is KtOperationExpression && parent.operationReference == expression ->
                return showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.expression"))
        }

        PsiTreeUtil.getNonStrictParentOfType(expression,
                                             KtTypeReference::class.java,
                                             KtConstructorCalleeExpression::class.java,
                                             KtSuperExpression::class.java)?.let {
            return showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.container"))
        }

        val expressionType = bindingContext.getType(expression) //can be null or error type
        val scope = expression.getResolutionScope(bindingContext, resolutionFacade)
        val dataFlowInfo = bindingContext.getDataFlowInfo(expression)

        val bindingTrace = ObservableBindingTrace(BindingTraceContext())
        val typeNoExpectedType = expression.computeTypeInfoInContext(scope, expression, bindingTrace, dataFlowInfo).type
        val noTypeInference = expressionType != null
                              && typeNoExpectedType != null
                              && !KotlinTypeChecker.DEFAULT.equalTypes(expressionType, typeNoExpectedType)

        if (expressionType == null && bindingContext.get(BindingContext.QUALIFIER, expression) != null) {
            return showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.package.expression"))
        }

        if (expressionType != null && KotlinBuiltIns.isUnit(expressionType)) {
            return showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.expression.has.unit.type"))
        }

        val isInplaceAvailable = editor != null
                                 && editor.settings.isVariableInplaceRenameEnabled
                                 && !ApplicationManager.getApplication().isUnitTestMode

        val allOccurrences = occurrencesToReplace ?: expression.findOccurrences(occurrenceContainer)

        val callback = Pass<OccurrencesChooser.ReplaceChoice> { replaceChoice ->
            val allReplaces = when (replaceChoice) {
                OccurrencesChooser.ReplaceChoice.ALL -> allOccurrences
                else -> listOf(expression)
            }
            val replaceOccurrence = expression.shouldReplaceOccurrence(bindingContext, container) || allReplaces.size > 1

            val commonParent = PsiTreeUtil.findCommonParent(allReplaces) as KtElement
            var commonContainer = commonParent.getContainer()!!
            if (commonContainer != container && container.isAncestor(commonContainer, true)) {
                commonContainer = container
            }

            val validator = NewDeclarationNameValidator(
                    commonContainer,
                    calculateAnchor(commonParent, commonContainer, allReplaces),
                    NewDeclarationNameValidator.Target.VARIABLES
            )
            val suggestedNames = KotlinNameSuggester.suggestNamesByExpressionAndType(expression, bindingContext, validator, "value")
            val introduceVariableContext = IntroduceVariableContext(
                    expression, suggestedNames.iterator().next(), allReplaces, commonContainer, commonParent,
                    replaceOccurrence, noTypeInference, expressionType, bindingContext, resolutionFacade
            )
            project.executeCommand(INTRODUCE_VARIABLE, null) {
                runWriteAction { introduceVariableContext.runRefactoring() }

                val property = introduceVariableContext.propertyRef ?: return@executeCommand

                if (editor == null) {
                    onNonInteractiveFinish?.invoke(property)
                    return@executeCommand
                }

                editor.caretModel.moveToOffset(property.textOffset)
                editor.selectionModel.removeSelection()

                if (!isInplaceAvailable) return@executeCommand

                PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
                KotlinVariableInplaceIntroducer(
                        property,
                        introduceVariableContext.reference,
                        introduceVariableContext.references.toTypedArray(),
                        suggestedNames,
                        /*todo*/ false,
                        /*todo*/ false,
                        expressionType,
                        noTypeInference,
                        project,
                        editor
                ).startInplaceIntroduceTemplate()
            }
        }

        if (isInplaceAvailable && occurrencesToReplace == null) {
            OccurrencesChooser.simpleChooser<KtExpression>(editor).showChooser(expression, allOccurrences, callback)
        }
        else {
            callback.pass(OccurrencesChooser.ReplaceChoice.ALL)
        }
    }

    private fun PsiElement.isFunExpressionOrLambdaBody(): Boolean {
        if (isFunctionalExpression()) return true
        val parent = parent as? KtFunction ?: return false
        return parent.bodyExpression == this && (parent is KtFunctionLiteral || parent.isFunctionalExpression())
    }

    private fun KtExpression.getCandidateContainers(
            resolutionFacade: ResolutionFacade,
            originalContext: BindingContext
    ): List<Pair<KtElement, KtElement>> {
        val file = getContainingKtFile()

        val references = collectDescendantsOfType<KtReferenceExpression>()

        fun isResolvableNextTo(neighbour: KtExpression): Boolean {
            val scope = neighbour.getResolutionScope(originalContext, resolutionFacade)
            val newContext = analyzeInContext(scope, neighbour)
            val project = file.project
            return references.all {
                val originalDescriptor = originalContext[BindingContext.REFERENCE_TARGET, it]
                val newDescriptor = newContext[BindingContext.REFERENCE_TARGET, it]

                if (originalDescriptor is ValueParameterDescriptor
                    && (originalContext[BindingContext.AUTO_CREATED_IT, originalDescriptor] ?: false)) {
                    return@all originalDescriptor.containingDeclaration.source.getPsi().isAncestor(neighbour, true)
                }

                compareDescriptors(project, newDescriptor, originalDescriptor)
            }
        }

        val firstContainer = getContainer() ?: return emptyList()
        val firstOccurrenceContainer = getOccurrenceContainer() ?: return emptyList()

        val containers = SmartList(firstContainer)
        val occurrenceContainers = SmartList(firstOccurrenceContainer)

        if (!firstContainer.isFunExpressionOrLambdaBody()) return listOf(firstContainer to firstOccurrenceContainer)

        val lambdasAndContainers = ArrayList<Pair<KtExpression, KtElement>>().apply {
            var container = firstContainer
            do {
                var lambda: KtExpression = container.getNonStrictParentOfType<KtFunction>()!!
                if (lambda is KtFunctionLiteral) lambda = lambda.parent as? KtFunctionLiteralExpression ?: return@apply
                if (!isResolvableNextTo(lambda)) return@apply
                container = lambda.getContainer() ?: return@apply
                add(lambda to container)
            } while (container.isFunExpressionOrLambdaBody())
        }

        lambdasAndContainers.mapTo(containers) { it.second }
        lambdasAndContainers.mapTo(occurrenceContainers) { it.first.getOccurrenceContainer() }
        return ArrayList<Pair<KtElement, KtElement>>().apply {
            for ((container, occurrenceContainer) in (containers zip occurrenceContainers)) {
                if (occurrenceContainer == null) continue
                add(container to occurrenceContainer)
            }
        }
    }

    fun doRefactoring(
            project: Project,
            editor: Editor?,
            expressionToExtract: KtExpression?,
            occurrencesToReplace: List<KtExpression>?,
            onNonInteractiveFinish: ((KtProperty) -> Unit)?
    ) {
        val expression = expressionToExtract?.let { KtPsiUtil.safeDeparenthesize(it) }
                         ?: return showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.expression"))

        val resolutionFacade = expression.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(expression, BodyResolveMode.FULL)

        fun runWithChosenContainers(container: KtElement, occurrenceContainer: KtElement) {
            doRefactoring(project, editor, expression, container, occurrenceContainer, resolutionFacade, bindingContext, occurrencesToReplace, onNonInteractiveFinish)
        }

        val candidateContainers = expression.getCandidateContainers(resolutionFacade, bindingContext).ifEmpty {
            return showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.container"))
        }

        if (editor == null) {
            return candidateContainers.first().let { runWithChosenContainers(it.first, it.second) }
        }

        if (ApplicationManager.getApplication().isUnitTestMode) {
            return candidateContainers.last().let { runWithChosenContainers(it.first, it.second) }
        }

        chooseContainerElementIfNecessary(candidateContainers, editor, "Select target code block", true, { it.first }) {
            runWithChosenContainers(it.first, it.second)
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) {
        try {
            KotlinRefactoringUtil.selectExpression(editor, file) { doRefactoring(project, editor, it, null, null) }
        }
        catch (e: KotlinRefactoringUtil.IntroduceRefactoringException) {
            showErrorHint(project, editor, e.message!!)
        }
    }

    override fun invoke(project: Project, elements: Array<PsiElement>, dataContext: DataContext) {
        //do nothing
    }
}
