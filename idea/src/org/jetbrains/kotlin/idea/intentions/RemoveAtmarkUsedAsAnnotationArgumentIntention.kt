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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.*

class RemoveAtmarkUsedAsAnnotationArgumentInspection : IntentionBasedInspection<KtAnnotated>(RemoveAtmarkUsedAsAnnotationArgumentIntention())

class RemoveAtmarkUsedAsAnnotationArgumentIntention : SelfTargetingRangeIntention<KtAnnotated>(KtAnnotated::class.java, "Remove @ used as annotation argument") {
    override fun applicabilityRange(element: KtAnnotated): TextRange? {
        val annotationEntries = element.annotationEntries
        val isApplicable = annotationEntries.firstOrNull {
            it.valueArguments.firstOrNull { hasAnnotatedExpression(it) } != null
        } != null
        return if (isApplicable) TextRange(annotationEntries.first().textRange.startOffset, annotationEntries.last().textRange.endOffset) else null
    }

    private fun hasAnnotatedExpression(expression: ValueArgument): Boolean {
        expression.getArgumentExpression()?.let {
            when (it) {
                is KtAnnotatedExpression -> return true
                is KtCallExpression -> return hasAnnotatedExpression(it)
                else -> return false
            }
        }
        return false
    }

    private fun hasAnnotatedExpression(expression: KtCallExpression): Boolean {
        expression.children.forEach {
            when (it) {
                is KtAnnotatedExpression -> return true
                is KtCallExpression -> return hasAnnotatedExpression(it)
                is KtValueArgument -> return hasAnnotatedExpression(it)
                is KtValueArgumentList -> {
                    return it.arguments.firstOrNull { hasAnnotatedExpression(it) } != null
                }
            }
        }
        return false
    }

    override fun applyTo(element: KtAnnotated, editor: Editor?) {
        element.annotationEntries.forEach {
            it.valueArguments.filter {
                hasAnnotatedExpression(it)
            }.forEach {
                removeAtmark(it)
            }
        }
    }

    private fun removeAtmark(expression: ValueArgument) {
        expression.getArgumentExpression()?.let {
            when (it) {
                is KtAnnotatedExpression -> it.removeAtmarkFromAnnotatedExpression()
                is KtCallExpression -> removeAtmark(it)
            }
        }
    }

    private fun removeAtmark(expression: KtCallExpression) {
        expression.children.forEach {
            when (it) {
                is KtAnnotatedExpression -> it.removeAtmarkFromAnnotatedExpression()
                is KtCallExpression -> removeAtmark(it)
                is KtValueArgument -> removeAtmark(it)
                is KtValueArgumentList -> {
                    it.arguments.filter {
                        hasAnnotatedExpression(it)
                    }.forEach {
                        removeAtmark(it)
                    }
                }
            }
        }
    }

    private fun KtAnnotatedExpression.removeAtmarkFromAnnotatedExpression() {
        val expression = KtPsiFactory(this.project).createExpression(this.text.replaceFirst("@", ""))
        this.replace(expression)
    }
}