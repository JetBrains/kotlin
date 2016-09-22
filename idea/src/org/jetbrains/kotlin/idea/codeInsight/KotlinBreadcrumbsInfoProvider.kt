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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.project.DumbService
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import com.intellij.usageView.UsageViewShortNameLocation
import com.intellij.xml.breadcrumbs.BreadcrumbsInfoProvider
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.unwrapIfLabeled
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentsInParentheses
import kotlin.reflect.KClass

class KotlinBreadcrumbsInfoProvider : BreadcrumbsInfoProvider() {
    private abstract class ElementHandler<TElement : KtElement>(val type: KClass<TElement>) {
        abstract fun elementInfo(element: TElement): String
        abstract fun elementTooltip(element: TElement): String

        open fun accepts(element: TElement): Boolean = true
    }

    private val handlers = listOf<ElementHandler<*>>(
            LambdaHandler,
            AnonymousObjectHandler,
            AnonymousFunctionHandler,
            PropertyAccessorHandler,
            DeclarationHandler,
            IfThenHandler,
            ElseHandler,
            TryHandler,
            CatchHandler,
            FinallyHandler,
            WhileHandler,
            DoWhileHandler,
            WhenHandler,
            WhenEntryHandler,
            ForHandler
    )

    private object LambdaHandler : ElementHandler<KtFunctionLiteral>(KtFunctionLiteral::class) {
        override fun elementInfo(element: KtFunctionLiteral): String {
            val lambdaExpression = element.parent as KtLambdaExpression
            val unwrapped = lambdaExpression.unwrapIfLabeled()
            val label = lambdaExpression.labelText()
            val lambdaText = "$label{$ellipsis}"

            val parent = unwrapped.parent
            when (parent) {
                is KtLambdaArgument -> {
                    val callExpression = parent.parent as? KtCallExpression
                    val callName = callExpression?.getCallNameExpression()?.getReferencedName()
                    if (callName != null) {
                        val receiverText = callExpression!!.getQualifiedExpressionForSelector()?.let {
                            it.receiverExpression.text.orEllipsis(TextKind.INFO) + it.operationSign.value
                        } ?: ""

                        return buildString {
                            append(receiverText)
                            append(callName)

                            if (callExpression.valueArgumentList != null) {
                                appendCallArguments(callExpression)
                            }
                            else {
                                if (label.isNotEmpty()) append(" ")
                            }
                            append(lambdaText)
                        }
                    }
                }

                is KtProperty -> {
                    val name = parent.nameAsName
                    if (unwrapped == parent.initializer && name != null) {
                        val valOrVar = if (parent.isVar) "var" else "val"
                        return "$valOrVar ${name.render()} = $lambdaText"
                    }
                }
            }

            return lambdaText
        }

        private fun StringBuilder.appendCallArguments(callExpression: KtCallExpression) {
            var argumentText = "($ellipsis)"
            val arguments = callExpression.getValueArgumentsInParentheses()
            when (arguments.size) {
                0 -> argumentText = "()"
                1 -> {
                    val argument = arguments.single()
                    val argumentExpression = argument.getArgumentExpression()
                    if (!argument.isNamed() && argument.getSpreadElement() == null && argumentExpression != null) {
                        argumentText = "(" + argumentExpression.shortText(TextKind.INFO) + ")"
                    }
                }
            }
            append(argumentText)
            append(" ")
        }

        //TODO
        override fun elementTooltip(element: KtFunctionLiteral): String {
            return ElementDescriptionUtil.getElementDescription(element, RefactoringDescriptionLocation.WITH_PARENT)
        }
    }

    private object AnonymousObjectHandler : ElementHandler<KtObjectDeclaration>(KtObjectDeclaration::class) {
        override fun accepts(element: KtObjectDeclaration) = element.isObjectLiteral()

        override fun elementInfo(element: KtObjectDeclaration) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: KtObjectDeclaration) = element.buildText(TextKind.TOOLTIP)

        private fun KtObjectDeclaration.buildText(kind: TextKind): String {
            return buildString {
                append("object")

                val superTypeEntries = getSuperTypeListEntries()
                if (superTypeEntries.isNotEmpty()) {
                    append(" : ")

                    if (kind == TextKind.INFO) {
                        val entry = superTypeEntries.first()
                        entry.typeReference?.text?.truncateStart(kind)?.let { append(it) }
                        if (superTypeEntries.size > 1) {
                            if (!endsWith(ellipsis)) {
                                append(",$ellipsis")
                            }
                        }
                    }
                    else {
                        append(superTypeEntries.joinToString(separator = ", ") { it.typeReference?.text ?: "" }.truncateEnd(kind))
                    }
                }
            }
        }
    }

    private object AnonymousFunctionHandler : ElementHandler<KtNamedFunction>(KtNamedFunction::class) {
        override fun accepts(element: KtNamedFunction) = element.name == null

        override fun elementInfo(element: KtNamedFunction) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: KtNamedFunction) = element.buildText(TextKind.TOOLTIP)

        private fun KtNamedFunction.buildText(kind: TextKind): String {
            return "fun(" +
                   valueParameters.joinToString(separator = ", ") { if (kind == TextKind.INFO) it.name ?: "" else it.text }.truncateEnd(kind) +
                   ")"
        }
    }

    private object PropertyAccessorHandler : ElementHandler<KtPropertyAccessor>(KtPropertyAccessor::class) {
        override fun elementInfo(element: KtPropertyAccessor): String {
            return DeclarationHandler.elementInfo(element.property) + "." + (if (element.isGetter) "get" else "set")
        }

        override fun elementTooltip(element: KtPropertyAccessor): String {
            return DeclarationHandler.elementTooltip(element)
        }
    }

    private object DeclarationHandler : ElementHandler<KtDeclaration>(KtDeclaration::class) {
        override fun accepts(element: KtDeclaration): Boolean {
            if (element is KtProperty) {
                return element.parent is KtFile || element.parent is KtClassBody // do not show local variables
            }
            return true
        }

        override fun elementInfo(element: KtDeclaration): String {
            when {
                element is KtProperty -> {
                    return (if (element.isVar) "var " else "val ") + element.nameAsName?.render()
                }

                element is KtObjectDeclaration && element.isCompanion() -> {
                    return buildString {
                        append("companion object")
                        element.nameIdentifier?.let { append(" "); append(it.text) }
                    }
                }

                else -> {
                    val description = ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE)
                    val suffix = if (element is KtFunction) "()" else null
                    return if (suffix != null) description + suffix else description
                }
            }

        }

        override fun elementTooltip(element: KtDeclaration): String {
            return ElementDescriptionUtil.getElementDescription(element, RefactoringDescriptionLocation.WITH_PARENT)
        }
    }

    private abstract class ConstructWithExpressionHandler<TElement : KtElement>(
            private val constructName: String,
            type: KClass<TElement>
    ) : ElementHandler<TElement>(type) {

        protected abstract fun extractExpression(element: TElement): KtExpression?
        protected abstract fun labelOwner(element: TElement): KtExpression?

        override fun elementInfo(element: TElement) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: TElement) = element.buildText(TextKind.TOOLTIP)

        protected fun TElement.buildText(kind: TextKind): String {
            return buildString {
                append(labelOwner(this@buildText)?.labelText() ?: "")
                append(constructName)
                val expression = extractExpression(this@buildText)
                if (expression != null) {
                    append(" (")
                    append(expression.shortText(kind))
                    append(")")
                }
            }
        }
    }

    private object IfThenHandler : ConstructWithExpressionHandler<KtContainerNode>("if", KtContainerNode::class) {
        override fun accepts(element: KtContainerNode): Boolean {
            return element.node.elementType == KtNodeTypes.THEN
        }

        override fun extractExpression(element: KtContainerNode): KtExpression? {
            return (element.parent as KtIfExpression).condition
        }

        override fun labelOwner(element: KtContainerNode) = null

        override fun elementInfo(element: KtContainerNode): String {
            return elseIfPrefix(element) + super.elementInfo(element)
        }

        override fun elementTooltip(element: KtContainerNode): String {
            return elseIfPrefix(element) + super.elementTooltip(element)
        }

        private fun elseIfPrefix(then: KtContainerNode): String {
            return if ((then.parent as KtIfExpression).isElseIf()) "if $ellipsis else " else ""
        }
    }

    private object ElseHandler : ElementHandler<KtContainerNode>(KtContainerNode::class) {
        override fun accepts(element: KtContainerNode): Boolean {
            return element.node.elementType == KtNodeTypes.ELSE
                   && (element.parent as KtIfExpression).`else` !is KtIfExpression // filter out "else if"
        }

        override fun elementInfo(element: KtContainerNode): String {
            val ifExpression = element.parent as KtIfExpression
            val then = ifExpression.thenNode
            val ifInfo = if (ifExpression.isElseIf() || then == null) "if" else IfThenHandler.elementInfo(then)
            return "$ifInfo $ellipsis else"
        }

        override fun elementTooltip(element: KtContainerNode): String {
            val ifExpression = element.parent as KtIfExpression
            val thenNode = ifExpression.thenNode ?: return "else"
            return "else (of '" + IfThenHandler.elementTooltip(thenNode) + "')" //TODO
        }

        private val KtIfExpression.thenNode: KtContainerNode?
            get() = children.firstOrNull { it.node.elementType == KtNodeTypes.THEN } as KtContainerNode?
    }

    private object TryHandler : ElementHandler<KtBlockExpression>(KtBlockExpression::class) {
        override fun accepts(element: KtBlockExpression) = element.parent is KtTryExpression

        override fun elementInfo(element: KtBlockExpression) = "try"
        override fun elementTooltip(element: KtBlockExpression) = "try"
    }

    private object CatchHandler : ElementHandler<KtCatchClause>(KtCatchClause::class) {
        override fun elementInfo(element: KtCatchClause): String {
            val text = element.catchParameter?.typeReference?.text ?: ""
            return "catch ($text)"
        }

        override fun elementTooltip(element: KtCatchClause): String {
            return elementInfo(element)
        }
    }

    private object FinallyHandler : ElementHandler<KtFinallySection>(KtFinallySection::class) {
        override fun elementInfo(element: KtFinallySection) = "finally"
        override fun elementTooltip(element: KtFinallySection) = "finally"
    }

    private object WhileHandler : ConstructWithExpressionHandler<KtContainerNode>("while", KtContainerNode::class) {
        override fun accepts(element: KtContainerNode) = element.bodyOwner() is KtWhileExpression
        override fun extractExpression(element: KtContainerNode) = (element.bodyOwner() as KtWhileExpression).condition
        override fun labelOwner(element: KtContainerNode) = element.bodyOwner()
    }

    private object DoWhileHandler : ConstructWithExpressionHandler<KtContainerNode>("do $ellipsis while", KtContainerNode::class) {
        override fun accepts(element: KtContainerNode) = element.bodyOwner() is KtDoWhileExpression
        override fun extractExpression(element: KtContainerNode) = (element.bodyOwner() as KtDoWhileExpression).condition
        override fun labelOwner(element: KtContainerNode) = element.bodyOwner()
    }

    private object WhenHandler : ConstructWithExpressionHandler<KtWhenExpression>("when", KtWhenExpression::class) {
        override fun extractExpression(element: KtWhenExpression) = element.subjectExpression
        override fun labelOwner(element: KtWhenExpression) = null
    }

    private object WhenEntryHandler : ElementHandler<KtExpression>(KtExpression::class) {
        override fun accepts(element: KtExpression) = element.parent is KtWhenEntry

        override fun elementInfo(element: KtExpression) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: KtExpression) = element.buildText(TextKind.TOOLTIP)

        private fun KtExpression.buildText(kind: TextKind): String {
            with (parent as KtWhenEntry) {
                if (isElse) {
                    return "else ->"
                }
                else {
                    val condition = conditions.firstOrNull() ?: return "->"
                    val firstConditionText = condition.buildText(kind)

                    if (conditions.size == 1) {
                        return firstConditionText + " ->"
                    }
                    else {
                        //TODO: show all conditions for tooltip
                        return (if (firstConditionText.endsWith(ellipsis)) firstConditionText else firstConditionText + ",$ellipsis") + " ->"
                    }
                }
            }
        }

        private fun KtWhenCondition.buildText(kind: TextKind): String {
            return when (this) {
                is KtWhenConditionIsPattern -> {
                    (if (isNegated) "!is" else "is") + " " + (typeReference?.text?.truncateEnd(kind) ?: "")
                }

                is KtWhenConditionInRange -> {
                    (if (isNegated) "!in" else "in") + " " + (rangeExpression?.text?.truncateEnd(kind) ?: "")
                }

                is KtWhenConditionWithExpression -> {
                    expression?.text?.truncateStart(kind) ?: ""
                }

                else -> error("Unknown when entry condition type: ${this}")
            }
        }
    }

    private object ForHandler : ElementHandler<KtContainerNode>(KtContainerNode::class) {
        override fun accepts(element: KtContainerNode) = element.bodyOwner() is KtForExpression

        override fun elementInfo(element: KtContainerNode) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: KtContainerNode) = element.buildText(TextKind.TOOLTIP)

        private fun KtContainerNode.buildText(kind: TextKind): String {
            with (bodyOwner() as KtForExpression) {
                val parameterText = loopParameter?.nameAsName?.render() ?: destructuringParameter?.text ?: return "for"
                val collectionText = loopRange?.text ?: ""
                val text = (parameterText + " in " + collectionText).truncateEnd(kind)
                return labelText() + "for($text)"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handler(e: PsiElement): ElementHandler<in KtElement>? {
        if (e !is KtElement) return null
        val handler = handlers.firstOrNull { it.type.java.isInstance(e) && (it as ElementHandler<in KtElement>).accepts(e) }
        return handler as ElementHandler<in KtElement>?
    }

    override fun getLanguages() = arrayOf(KotlinLanguage.INSTANCE)

    override fun acceptElement(e: PsiElement) = !DumbService.isDumb(e.project) && handler(e) != null

    override fun getElementInfo(e: PsiElement): String {
        if (DumbService.isDumb(e.project)) return ""
        return handler(e)!!.elementInfo(e as KtElement)
    }

    override fun getElementTooltip(e: PsiElement): String {
        if (DumbService.isDumb(e.project)) return ""
        return handler(e)!!.elementTooltip(e as KtElement)
    }

    override fun getParent(e: PsiElement): PsiElement? {
        val node = e.node ?: return null
        when (node.elementType) {
            KtNodeTypes.PROPERTY_ACCESSOR ->
                return e.parent.parent

            else ->
                return e.parent
        }
    }

    private companion object {
        enum class TextKind(val maxTextLength: Int) {
            INFO(16), TOOLTIP(100)
        }

        fun KtExpression.shortText(kind: TextKind): String {
            return if (this is KtNameReferenceExpression) text else text.truncateEnd(kind)
        }

        //TODO: line breaks

        fun String.orEllipsis(kind: TextKind): String {
            return if (length <= kind.maxTextLength) this else ellipsis
        }

        fun String.truncateEnd(kind: TextKind): String {
            val maxLength = kind.maxTextLength
            return if (length > maxLength) substring(0, maxLength - ellipsis.length) + ellipsis else this
        }

        fun String.truncateStart(kind: TextKind): String {
            val maxLength = kind.maxTextLength
            return if (length > maxLength) ellipsis + substring(length - maxLength - 1) else this
        }

        val ellipsis = "${Typography.ellipsis}"

        fun KtIfExpression.isElseIf() = parent.node.elementType == KtNodeTypes.ELSE

        fun KtContainerNode.bodyOwner(): KtExpression? {
            return if (node.elementType == KtNodeTypes.BODY) parent as KtExpression else null
        }

        fun KtExpression.labelText(): String {
            var result = ""
            var current = parent
            while (current is KtLabeledExpression) {
                result = current.getLabelName() + "@ " + result
                current = current.parent
            }
            return result
        }
    }
}
