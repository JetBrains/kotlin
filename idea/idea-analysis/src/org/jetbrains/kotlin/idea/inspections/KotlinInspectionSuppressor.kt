/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.SuppressManager
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.highlighter.createSuppressWarningActions

class KotlinInspectionSuppressor : InspectionSuppressor {
    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        element ?: return emptyArray()
        return createSuppressWarningActions(element, Severity.WARNING, toolId).map {
            object : SuppressQuickFix {
                override fun getFamilyName() = it.familyName

                override fun applyFix(project: Project, descriptor: ProblemDescriptor) = it.invoke(project, null, descriptor.psiElement)

                override fun isAvailable(project: Project, context: PsiElement) = it.isAvailable(project, null, context)

                override fun isSuppressAll() = it.isSuppressAll
            }
        }.toTypedArray()
    }

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (SuppressManager.getInstance()!!.isSuppressedFor(element, toolId)) {
            return true
        }

        if (KotlinCacheService.getInstance(element.project).getSuppressionCache().isSuppressed(element, toolId, Severity.WARNING)) {
            return true
        }

        return false
    }
}
