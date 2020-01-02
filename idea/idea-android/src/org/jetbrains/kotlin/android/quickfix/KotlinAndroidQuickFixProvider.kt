/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.quickfix

import com.android.SdkConstants.SUPPORT_ANNOTATIONS_PREFIX
import com.android.tools.lint.checks.ApiDetector
import com.android.tools.lint.checks.CommentDetector
import com.android.tools.lint.checks.ParcelDetector
import com.android.tools.lint.detector.api.Issue
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.android.inspections.lint.AndroidLintQuickFix
import org.jetbrains.android.inspections.lint.AndroidLintQuickFixProvider
import java.util.regex.Pattern


class KotlinAndroidQuickFixProvider : AndroidLintQuickFixProvider {
    override fun getQuickFixes(
        issue: Issue,
        startElement: PsiElement,
        endElement: PsiElement,
        message: String,
        data: Any?
    ): Array<AndroidLintQuickFix> {
        val fixes: Array<AndroidLintQuickFix> = when (issue) {
            ApiDetector.UNSUPPORTED, ApiDetector.INLINED -> getApiQuickFixes(issue, startElement, message)
            ParcelDetector.ISSUE -> arrayOf(ParcelableQuickFix())
            else -> emptyArray()
        }

        if (issue != CommentDetector.STOP_SHIP) {
            return fixes + SuppressLintQuickFix(issue.id)
        }

        return fixes
    }

    fun getApiQuickFixes(issue: Issue, element: PsiElement, message: String): Array<AndroidLintQuickFix> {
        val api = getRequiredVersion(message)
        if (api == -1) {
            return AndroidLintQuickFix.EMPTY_ARRAY
        }

        val project = element.project
        if (JavaPsiFacade.getInstance(project).findClass(REQUIRES_API_ANNOTATION, GlobalSearchScope.allScope(project)) != null) {
            return arrayOf(AddTargetApiQuickFix(api, true), AddTargetApiQuickFix(api, false), AddTargetVersionCheckQuickFix(api))
        }

        return arrayOf(AddTargetApiQuickFix(api, false), AddTargetVersionCheckQuickFix(api))
    }

    private fun getRequiredVersion(errorMessage: String): Int {
        val pattern = Pattern.compile("\\s(\\d+)\\s")
        val matcher = pattern.matcher(errorMessage)
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1))
        }

        return -1
    }

    companion object {
        val REQUIRES_API_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "RequiresApi"
    }
}