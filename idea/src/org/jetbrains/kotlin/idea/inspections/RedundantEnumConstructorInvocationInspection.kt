/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.enumEntryVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RedundantEnumConstructorInvocationInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = enumEntryVisitor(fun(enumEntry) {
        val valueArgumentList = enumEntry.valueArgumentListIfEmpty() ?: return
        holder.registerProblem(
            valueArgumentList,
            "Redundant enum constructor invocation",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
            RemoveEnumConstructorInvocationFix()
        )
    })
}

private class RemoveEnumConstructorInvocationFix : LocalQuickFix {
    override fun getName() = "Remove enum constructor invocation"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        descriptor.psiElement.getStrictParentOfType<KtEnumEntry>()?.containingClass()?.body?.getChildrenOfType<KtEnumEntry>()?.forEach {
            it.valueArgumentListIfEmpty()?.delete()
        }
    }
}

private fun KtEnumEntry.valueArgumentListIfEmpty(): KtValueArgumentList? {
    val superTypeCallEntry = initializerList?.initializers?.singleOrNull() as? KtSuperTypeCallEntry ?: return null
    val valueArgumentList = superTypeCallEntry.valueArgumentList ?: return null
    if (valueArgumentList.arguments.isNotEmpty()) return null
    if (valueArgumentList.analyze().diagnostics.forElement(valueArgumentList).any { it.severity == Severity.ERROR }) return null
    return valueArgumentList
}