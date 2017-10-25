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
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.descriptors.accessors
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinSuspendCallLineMarkerProvider : LineMarkerProvider {
    private class SuspendCallMarkerInfo(callElement: PsiElement, message: String) : LineMarkerInfo<PsiElement>(
            callElement,
            callElement.textRange,
            KotlinIcons.SUSPEND_CALL,
            Pass.UPDATE_OVERRIDDEN_MARKERS,
            { message },
            null,
            GutterIconRenderer.Alignment.RIGHT
    ) {
        override fun createGutterRenderer(): GutterIconRenderer? {
            return object : LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>(this) {
                override fun getClickAction(): AnAction? = null
            }
        }
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
            elements: MutableList<PsiElement>,
            result: MutableCollection<LineMarkerInfo<*>>
    ) {
        val markedLineNumbers = HashSet<Int>()

        for (element in elements) {
            ProgressManager.checkCanceled()

            if (element !is KtExpression) continue
            val lineNumber = element.getLineNumber()
            if (lineNumber in markedLineNumbers) continue
            if (!element.hasSuspendCalls()) continue

            markedLineNumbers += lineNumber
            result += if (element is KtForExpression) {
                SuspendCallMarkerInfo(getElementForLineMark(element.loopRange!!), "Suspending iteration")
            } else {
                SuspendCallMarkerInfo(getElementForLineMark(element), "Suspend function call")
            }
        }
    }
}

private fun KtExpression.isValidCandidateExpression(): Boolean {
    if (this is KtOperationReferenceExpression || this is KtForExpression || this is KtProperty) return true
    val parent = parent
    if (parent is KtCallExpression && parent.calleeExpression == this) return true
    return false
}

fun KtExpression.hasSuspendCalls(bindingContext: BindingContext = analyze(BodyResolveMode.PARTIAL)): Boolean {
    if (!isValidCandidateExpression()) return false

    return when (this) {
        is KtForExpression -> {
            val iteratorResolvedCall = bindingContext[LOOP_RANGE_ITERATOR_RESOLVED_CALL, loopRange]
            val loopRangeHasNextResolvedCall = bindingContext[LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, loopRange]
            val loopRangeNextResolvedCall = bindingContext[LOOP_RANGE_NEXT_RESOLVED_CALL, loopRange]
            listOf(iteratorResolvedCall, loopRangeHasNextResolvedCall, loopRangeNextResolvedCall).any {
                it?.resultingDescriptor?.isSuspend == true
            }
        }
        is KtProperty -> {
            if (hasDelegateExpression()) {
                val variableDescriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, this] as? VariableDescriptorWithAccessors
                val accessors = variableDescriptor?.accessors ?: emptyList()
                accessors.any { accessor ->
                    val delegatedFunctionDescriptor = bindingContext[DELEGATED_PROPERTY_RESOLVED_CALL, accessor]?.resultingDescriptor
                    delegatedFunctionDescriptor?.isSuspend == true
                }
            }
            else {
                false
            }
        }
        else -> {
            val resolvedCall = getResolvedCall(bindingContext)
            (resolvedCall?.resultingDescriptor as? FunctionDescriptor)?.isSuspend == true
        }
    }
}
