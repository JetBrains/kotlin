/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinSuspendCallLineMarkerProvider : LineMarkerProvider {
    private class SuspendCallMarkerInfo(callElement: KtElement) : LineMarkerInfo<KtElement>(
            callElement,
            callElement.textRange,
            KotlinIcons.SUSPEND_CALL,
            Pass.UPDATE_OVERRIDEN_MARKERS,
            { "Suspend function call" },
            null,
            GutterIconRenderer.Alignment.RIGHT
    ) {
        override fun createGutterRenderer(): GutterIconRenderer? {
            return object : LineMarkerInfo.LineMarkerGutterIconRenderer<KtElement>(this) {
                override fun getClickAction() = null
            }
        }
    }

    override fun getLineMarkerInfo(element: PsiElement) = null

    private fun isValidCandidateExpression(expression: KtExpression): Boolean {
        if (expression is KtOperationReferenceExpression) return true
        val parent = expression.parent
        if (parent is KtCallExpression && parent.calleeExpression == expression) return true
        return false
    }

    override fun collectSlowLineMarkers(
            elements: MutableList<PsiElement>,
            result: MutableCollection<LineMarkerInfo<*>>
    ) {
        val markedLineNumbers = HashSet<Int>()

        for (element in elements) {
            ProgressManager.checkCanceled()

            if (element !is KtExpression) continue
            if (!isValidCandidateExpression(element)) continue

            val lineNumber = element.getLineNumber()
            if (lineNumber in markedLineNumbers) continue

            val resolvedCall = element.getResolvedCall(element.analyze(BodyResolveMode.PARTIAL)) ?: continue
            val calleeDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: continue
            if (!calleeDescriptor.isSuspend) continue

            markedLineNumbers += lineNumber
            result += SuspendCallMarkerInfo(element)
        }
    }
}
