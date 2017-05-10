/*
 * Copyright 2010-2017 JetBrains s.r.o.
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