/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class AnnotationTargetExpressionInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return classVisitor(fun(element: KtClass) {
            if (!element.isAnnotation()) return
            val target = element.targetAnnotationArgument()?.asElement() ?: return
            if (element.retentionAnnotation() == null) return

            val start = element.startOffset
            val highlightRange = TextRange(target.startOffset - start, target.endOffset - start)
            holder.registerProblem(
                holder.manager.createProblemDescriptor(
                    element,
                    highlightRange,
                    "annotation will not be stored",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly,
                    ChangeAnnotationRetentionToSourceFix()
                )
            )
        })
    }
}

private class ChangeAnnotationRetentionToSourceFix : LocalQuickFix {

    private val source = AnnotationRetention.SOURCE.name

    override fun getName() = "Change @Retention to $source"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? KtClass ?: return
        val retention = element.retentionAnnotation() ?: return

        val annotationTarget = "${KotlinBuiltIns.FQ_NAMES.annotationRetention.shortName().asString()}.$source"
        val psiFactory = KtPsiFactory(element)
        if (retention.valueArgumentList == null) {
            retention.add(psiFactory.createCallArguments("($annotationTarget)"))
        } else {
            if (retention.valueArguments.isNotEmpty()) retention.valueArgumentList?.removeArgument(0)
            retention.valueArgumentList?.addArgument(psiFactory.createArgument(annotationTarget))
        }
    }
}

private fun KtClass.targetAnnotationArgument(): ValueArgument? {
    val annotationName = KotlinBuiltIns.FQ_NAMES.target.shortName().asString()
    return annotationEntries
        .find { it.typeReference?.text == annotationName }
        ?.valueArguments
        ?.find { it.shortName() == AnnotationTarget.EXPRESSION.name }
}

private fun KtClass.retentionAnnotation(): KtAnnotationEntry? {
    val annotationName = KotlinBuiltIns.FQ_NAMES.retention.shortName().asString()
    val annotation = annotationEntries.find { it.typeReference?.text == annotationName } ?: return null
    return if (annotation.valueArguments.none { it.shortName() == AnnotationRetention.SOURCE.name }) annotation else null
}

private fun ValueArgument.shortName(): String? {
    return getArgumentExpression()?.resolveToCall()?.resultingDescriptor?.fqNameSafe?.shortName()?.asString()
}
