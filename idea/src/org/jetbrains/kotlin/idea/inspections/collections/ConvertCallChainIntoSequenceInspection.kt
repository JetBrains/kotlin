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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

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
        val calls = expression.collectCallExpression().reversed()
        val firstCall = calls.firstOrNull() ?: return
        val lastCall = calls.lastOrNull() ?: return
        val first = firstCall.parent as? KtQualifiedExpression ?: return
        val last = lastCall.parent as? KtQualifiedExpression ?: return
        val next = last.parent as? KtQualifiedExpression
        val nextType = next?.getCallableDescriptor()?.returnType

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

        val nextDescriptor = next?.getCallableDescriptor()
        if (nextDescriptor == null
            || !nextDescriptor.fqNameSafe.asString().startsWith("kotlin.sequences.")
            || nextDescriptor.returnType != nextType
        ) {
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

    val calls = collectCallExpression()
    if (calls.isEmpty()) return null

    val qualified = calls.first().parent as? KtQualifiedExpression ?: return null
    val fqName = qualified.getCallableDescriptor()?.returnType?.constructor?.declarationDescriptor?.fqNameSafe
    if (fqName != KotlinBuiltIns.FQ_NAMES.list) return null
    return qualified to calls.last()
}

private fun KtQualifiedExpression.collectCallExpression(): List<KtCallExpression> {
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
        .dropWhile { !it.isTransformation() }
        .takeWhile { it.isTransformation() && !it.hasReturn() }
        .toList()
    if (transformationCalls.size < 2) return emptyList()

    return transformationCalls
}

private fun KtCallExpression.hasReturn(): Boolean = valueArguments.any { arg ->
    arg.anyDescendantOfType<KtReturnExpression> { it.labelQualifier == null }
}

private fun KtCallExpression.isTransformation(): Boolean {
    val fqName = transformations[calleeExpression?.text] ?: return false
    return fqName == getCallableDescriptor()?.fqNameSafe
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
    "zipWithNext"
).associate { it to FqName("kotlin.collections.$it") }
