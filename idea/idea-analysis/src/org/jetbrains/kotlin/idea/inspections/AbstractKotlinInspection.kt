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
import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInspection.CustomSuppressableInspectionTool
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.codeInspection.SuppressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.KotlinCacheService

public abstract class AbstractKotlinInspection: LocalInspectionTool(), CustomSuppressableInspectionTool {
    public override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction>? {
        return SuppressManager.getInstance()!!.createSuppressActions(HighlightDisplayKey.find(getShortName())!!)
    }

    public override fun isSuppressedFor(element: PsiElement): Boolean {
        if (SuppressManager.getInstance()!!.isSuppressedFor(element, getID())) {
            return true
        }

        val project = element.project
        if (KotlinCacheService.getInstance(project).getSuppressionCache().isSuppressed(element, this.shortName, toSeverity(defaultLevel))) {
            return true
        }

        return false
    }

    public fun toSeverity(highlightDisplayLevel: HighlightDisplayLevel): Severity  {
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
}
