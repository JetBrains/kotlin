/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.psi.PsiWhiteSpace
import com.intellij.ui.EditorTextField
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import java.awt.BorderLayout
import javax.swing.JPanel

class ConvertCallChainIntoSequenceInspection : AbstractKotlinInspection() {

    private val defaultCallChainLength = 5

    private var callChainLength = defaultCallChainLength

    var callChainLengthText = defaultCallChainLength.toString()
        set(value) {
            field = value
            callChainLength = value.toIntOrNull() ?: defaultCallChainLength
        }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        qualifiedExpressionVisitor(fun(expression) {
            val (qualified, firstCall, callChainLength) = expression.findCallChain() ?: return
            val rangeInElement = firstCall.calleeExpression?.textRange?.shiftRight(-qualified.startOffset) ?: return
            val highlightType = if (callChainLength >= this.callChainLength)
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            else
                ProblemHighlightType.INFORMATION

            holder.registerProblemWithoutOfflineInformation(
                qualified,
                "Call chain on collection could be converted into 'Sequence' to improve performance",
                isOnTheFly,
                highlightType,
                rangeInElement,
                ConvertCallChainIntoSequenceFix()
            )
        })

    override fun createOptionsPanel(): JPanel = OptionsPanel(this)

    private class OptionsPanel(owner: ConvertCallChainIntoSequenceInspection) : JPanel() {
        init {
            layout = BorderLayout()
            val regexField = EditorTextField(owner.callChainLengthText).apply { setOneLineMode(true) }
            regexField.document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(e: DocumentEvent) {
                    owner.callChainLengthText = regexField.text
                }
            })
            val labeledComponent = LabeledComponent.create(regexField, "Call chain length to transform:", BorderLayout.WEST)
            add(labeledComponent, BorderLayout.NORTH)
        }
    }
}

private class ConvertCallChainIntoSequenceFix : LocalQuickFix {
    override fun getName() = "Convert call chain into 'Sequence'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expression = descriptor.psiElement as? KtQualifiedExpression ?: return
        val context = expression.analyze(BodyResolveMode.PARTIAL)
        val calls = expression.collectCallExpression(context).reversed()
        val firstCall = calls.firstOrNull() ?: return
        val lastCall = calls.lastOrNull() ?: return
        val first = firstCall.getQualifiedExpressionForSelector() ?: firstCall
        val last = lastCall.getQualifiedExpressionForSelector() ?: return
        val endWithTermination = lastCall.isTermination(context)

        val psiFactory = KtPsiFactory(expression)
        val dot = buildString {
            if (first is KtQualifiedExpression
                && first.receiverExpression.siblings().filterIsInstance<PsiWhiteSpace>().any { it.textContains('\n') }
            ) append("\n")
            if (first is KtSafeQualifiedExpression) append("?")
            append(".")
        }

        val firstCommentSaver = CommentSaver(first)
        val firstReplaced = first.replaced(
            psiFactory.buildExpression {
                if (first is KtQualifiedExpression) {
                    appendExpression(first.receiverExpression)
                    appendFixedText(dot)
                }
                appendExpression(psiFactory.createExpression("asSequence()"))
                appendFixedText(dot)
                appendExpression(firstCall)
            }
        )
        firstCommentSaver.restore(firstReplaced)

        if (!endWithTermination) {
            val lastCommentSaver = CommentSaver(last)
            val lastReplaced = last.replace(
                psiFactory.buildExpression {
                    appendExpression(last)
                    appendFixedText(dot)
                    appendExpression(psiFactory.createExpression("toList()"))
                }
            )
            lastCommentSaver.restore(lastReplaced)
        }
    }
}

private data class CallChain(
    val qualified: KtQualifiedExpression,
    val firstCall: KtCallExpression,
    val callChainLength: Int
)

private fun KtQualifiedExpression.findCallChain(): CallChain? {
    if (parent is KtQualifiedExpression) return null

    val context = analyze(BodyResolveMode.PARTIAL)
    val calls = collectCallExpression(context)
    if (calls.isEmpty()) return null

    val lastCall = calls.last()
    val receiverType =
        (lastCall.getQualifiedExpressionForSelector())?.receiverExpression?.getResolvedCall(context)?.resultingDescriptor?.returnType
            ?: lastCall.implicitReceiver(context)?.type
    if (receiverType?.isIterable(DefaultBuiltIns.Instance) != true) return null

    val firstCall = calls.first()
    val qualified = firstCall.getQualifiedExpressionForSelector() ?: firstCall.getQualifiedExpressionForReceiver() ?: return null
    return CallChain(qualified, lastCall, calls.size)
}

private fun KtQualifiedExpression.collectCallExpression(context: BindingContext): List<KtCallExpression> {
    val calls = mutableListOf<KtCallExpression>()

    fun collect(qualified: KtQualifiedExpression) {
        val call = qualified.callExpression ?: return
        calls.add(call)
        val receiver = qualified.receiverExpression
        if (receiver is KtCallExpression && receiver.implicitReceiver(context) != null) {
            calls.add(receiver)
            return
        }
        if (receiver is KtQualifiedExpression) collect(receiver)
    }
    collect(this)

    if (calls.size < 2) return emptyList()

    val transformationCalls = calls
        .asSequence()
        .dropWhile { !it.isTransformationOrTermination(context) }
        .takeWhile { it.isTransformationOrTermination(context) && !it.hasReturn() }
        .toList()
        .dropLastWhile { it.isLazyTermination(context) }
    if (transformationCalls.size < 2) return emptyList()

    return transformationCalls
}

private fun KtExpression.implicitReceiver(context: BindingContext): ImplicitReceiver? {
    return getResolvedCall(context)?.getImplicitReceiverValue()
}

private fun KtCallExpression.hasReturn(): Boolean = valueArguments.any { arg ->
    arg.anyDescendantOfType<KtReturnExpression> { it.labelQualifier == null }
}

private fun KtCallExpression.isTransformationOrTermination(context: BindingContext): Boolean {
    val fqName = transformationAndTerminations[calleeExpression?.text] ?: return false
    return isCalling(fqName, context)
}

private fun KtCallExpression.isTermination(context: BindingContext): Boolean {
    val fqName = terminations[calleeExpression?.text] ?: return false
    return isCalling(fqName, context)
}

private fun KtCallExpression.isLazyTermination(context: BindingContext): Boolean {
    val fqName = lazyTerminations[calleeExpression?.text] ?: return false
    return isCalling(fqName, context)
}

private val transformations = listOf(
    "chunked",
    "distinct",
    "distinctBy",
    "drop",
    "dropWhile",
    "filter",
    "filterIndexed",
    "filterIsInstance",
    "filterNot",
    "filterNotNull",
    "flatten",
    "map",
    "mapIndexed",
    "mapIndexedNotNull",
    "mapNotNull",
    "minus",
    "minusElement",
    "onEach",
    "plus",
    "plusElement",
    "requireNoNulls",
    "sorted",
    "sortedBy",
    "sortedByDescending",
    "sortedDescending",
    "sortedWith",
    "take",
    "takeWhile",
    "windowed",
    "withIndex",
    "zipWithNext"
).associate { it to FqName("kotlin.collections.$it") }

private val terminations = listOf(
    "all",
    "any",
    "asIterable",
    "asSequence",
    "associate",
    "associateBy",
    "associateByTo",
    "associateTo",
    "average",
    "contains",
    "count",
    "elementAt",
    "elementAtOrElse",
    "elementAtOrNull",
    "filterIndexedTo",
    "filterIsInstanceTo",
    "filterNotNullTo",
    "filterNotTo",
    "filterTo",
    "find",
    "findLast",
    "first",
    "firstOrNull",
    "fold",
    "foldIndexed",
    "groupBy",
    "groupByTo",
    "groupingBy",
    "indexOf",
    "indexOfFirst",
    "indexOfLast",
    "joinTo",
    "joinToString",
    "last",
    "lastIndexOf",
    "lastOrNull",
    "mapIndexedNotNullTo",
    "mapIndexedTo",
    "mapNotNullTo",
    "mapTo",
    "max",
    "maxBy",
    "maxWith",
    "min",
    "minBy",
    "minWith",
    "none",
    "partition",
    "reduce",
    "reduceIndexed",
    "single",
    "singleOrNull",
    "sum",
    "sumBy",
    "sumByDouble",
    "toCollection",
    "toHashSet",
    "toList",
    "toMutableList",
    "toMutableSet",
    "toSet",
    "toSortedSet",
    "unzip"
).associate {
    val pkg = if (it in listOf("contains", "indexOf", "lastIndexOf")) "kotlin.collections.List" else "kotlin.collections"
    it to FqName("$pkg.$it")
}

private val lazyTerminations = terminations.filter { (key, _) -> key == "groupingBy" }

private val transformationAndTerminations = transformations + terminations
