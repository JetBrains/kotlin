/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class ConvertCallChainIntoSequenceInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        qualifiedExpressionVisitor(fun(expression) {
            val (targetQualified, targetCall) = expression.findTarget() ?: return
            val rangeInElement = targetCall.calleeExpression?.textRange?.shiftRight(-targetQualified.startOffset) ?: return
            holder.registerProblem(
                holder.manager.createProblemDescriptor(
                    targetQualified,
                    rangeInElement,
                    "Call chain on collection should be converted into 'Sequence'",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly,
                    ConvertCallChainIntoSequenceFix()
                )
            )
        })
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
        val first = firstCall.getQualifiedExpressionForSelector() ?: return
        val last = lastCall.getQualifiedExpressionForSelector() ?: return
        val endWithTermination = lastCall.isTermination(context)

        val psiFactory = KtPsiFactory(expression)
        val dot = buildString {
            if (first.receiverExpression.siblings().filterIsInstance<PsiWhiteSpace>().any { it.textContains('\n') }) append("\n")
            if (first is KtSafeQualifiedExpression) append("?")
            append(".")
        }

        val firstCommentSaver = CommentSaver(first)
        val firstReplaced = first.replaced(
            psiFactory.buildExpression {
                appendExpression(first.receiverExpression)
                appendFixedText(dot)
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

private fun KtQualifiedExpression.findTarget(): Pair<KtQualifiedExpression, KtCallExpression>? {
    if (parent is KtQualifiedExpression) return null

    val context = analyze(BodyResolveMode.PARTIAL)
    val calls = collectCallExpression(context)
    if (calls.isEmpty()) return null

    val receiverType =
        (calls.last().getQualifiedExpressionForSelector())?.receiverExpression?.getResolvedCall(context)?.resultingDescriptor?.returnType
    if (receiverType?.isCollection() != true) return null

    val qualified = calls.first().getQualifiedExpressionForSelector() ?: return null
    return qualified to calls.last()
}

private fun KtQualifiedExpression.collectCallExpression(context: BindingContext): List<KtCallExpression> {
    val calls = mutableListOf<KtCallExpression>()

    fun collect(qualified: KtQualifiedExpression) {
        val call = qualified.callExpression ?: return
        calls.add(call)
        val receiver = qualified.receiverExpression
        if (receiver is KtQualifiedExpression) collect(receiver)
    }
    collect(this)

    if (calls.size < 2) return emptyList()

    val transformationCalls = calls
        .asSequence()
        .dropWhile { !it.isTransformationOrTermination(context) }
        .takeWhile { it.isTransformationOrTermination(context) && !it.hasReturn() }
        .toList()
    if (transformationCalls.size < 2) return emptyList()

    return transformationCalls
}

private fun KotlinType.isCollection(): Boolean {
    val classDescriptor = constructor.declarationDescriptor as? ClassDescriptor ?: return false
    val className = classDescriptor.name.asString()
    val builtIns = DefaultBuiltIns.Instance
    return className.endsWith("List") && classDescriptor.isSubclassOf(builtIns.list)
            || className.endsWith("Set") && classDescriptor.isSubclassOf(builtIns.set)
}

private fun KtCallExpression.hasReturn(): Boolean = valueArguments.any { arg ->
    arg.anyDescendantOfType<KtReturnExpression> { it.labelQualifier == null }
}

private fun KtCallExpression.isTransformationOrTermination(context: BindingContext): Boolean {
    val fqName = transformationAndTerminations[calleeExpression?.text] ?: return false
    return fqName == getResolvedCall(context)?.resultingDescriptor?.fqNameSafe
}

private fun KtCallExpression.isTermination(context: BindingContext): Boolean {
    val fqName = terminations[calleeExpression?.text] ?: return false
    return fqName == getResolvedCall(context)?.resultingDescriptor?.fqNameSafe
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
    "toSortedSet"
).associate {
    val pkg = if (it in listOf("contains", "indexOf", "lastIndexOf")) "kotlin.collections.List" else "kotlin.collections"
    it to FqName("$pkg.$it")
}

private val transformationAndTerminations = transformations + terminations
