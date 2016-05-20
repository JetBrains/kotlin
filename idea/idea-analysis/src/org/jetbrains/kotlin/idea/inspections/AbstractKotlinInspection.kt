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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.*
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.highlighter.createSuppressWarningActions

abstract class AbstractKotlinInspection: LocalInspectionTool(), CustomSuppressableInspectionTool {
    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction>? {
        if (element == null) return emptyArray()

        return createSuppressWarningActions(element, toSeverity(defaultLevel), suppressionKey).toTypedArray()
    }

    override fun isSuppressedFor(element: PsiElement): Boolean {
        if (SuppressManager.getInstance()!!.isSuppressedFor(element, id)) {
            return true
        }

        val project = element.project
        if (KotlinCacheService.getInstance(project).getSuppressionCache().isSuppressed(element, suppressionKey, toSeverity(defaultLevel))) {
            return true
        }

        return false
    }

    protected open val suppressionKey: String get() = this.shortName.removePrefix("Kotlin")
}

private fun toSeverity(highlightDisplayLevel: HighlightDisplayLevel): Severity  {
    return when (highlightDisplayLevel) {
        HighlightDisplayLevel.DO_NOT_SHOW -> Severity.INFO

        HighlightDisplayLevel.WARNING,
        HighlightDisplayLevel.WEAK_WARNING -> Severity.WARNING

        HighlightDisplayLevel.ERROR,
        HighlightDisplayLevel.GENERIC_SERVER_ERROR_OR_WARNING,
        HighlightDisplayLevel.NON_SWITCHABLE_ERROR -> Severity.ERROR

        else -> Severity.ERROR
    }
}

@Suppress("unused")
fun Array<ProblemDescriptor>.registerWithElementsUnwrapped(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        quickFixSubstitutor: ((LocalQuickFix, PsiElement) -> LocalQuickFix?)? = null) {
    forEach { problem ->
        @Suppress("UNCHECKED_CAST")
        val originalFixes = problem.fixes as? Array<LocalQuickFix> ?: LocalQuickFix.EMPTY_ARRAY
        val newElement = problem.psiElement.unwrapped ?: return@forEach
        val newFixes = quickFixSubstitutor?.let { subst ->
            originalFixes.mapNotNull { subst(it, newElement) }.toTypedArray()
        } ?: originalFixes
        val descriptor = holder.manager.createProblemDescriptor(newElement, problem.descriptionTemplate, isOnTheFly, newFixes, problem.highlightType)
        holder.registerProblem(descriptor)
    }
}