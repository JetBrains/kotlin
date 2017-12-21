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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.AsSequenceTransformation
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class LoopToCallChainInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                val nonLazyIntention = LoopToCallChainIntention()
                val lazyIntention = LoopToLazyCallChainIntention()

                override fun visitForExpression(expression: KtForExpression) {
                    super.visitForExpression(expression)

                    val nonLazyApplicable = nonLazyIntention.applicabilityRange(expression) != null
                    val lazyApplicable = lazyIntention.applicabilityRange(expression) != null

                    if (!nonLazyApplicable && !lazyApplicable) return

                    val fixes = mutableListOf<Fix>()
                    if (nonLazyApplicable) {
                        fixes += Fix(lazy = false, text = nonLazyIntention.text)
                    }
                    if (lazyApplicable) {
                        fixes += Fix(lazy = true, text = lazyIntention.text)
                    }

                    holder.registerProblem(
                            expression.forKeyword,
                            "Loop can be replaced with stdlib operations",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            *fixes.toTypedArray()
                    )
                }
            }

    class Fix(val lazy: Boolean, val text: String = "") : LocalQuickFix {
        override fun getFamilyName(): String {
            return if (lazy) {
                "Replace with stdlib operations with use of 'asSequence()'"
            }
            else {
                "Replace with stdlib operations"
            }
        }

        override fun getName() = text

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement.getParentOfType<KtForExpression>(strict = true) ?: return
            applyFix(expression)
        }

        fun applyFix(expression: KtForExpression, editor: Editor? = expression.findExistingEditor()) {
            val match = match(expression, lazy, true) ?: return
            val result = convertLoop(expression, match)

            val offset = when (result) {
                // if result is variable declaration, put the caret onto its name to allow quick inline
                is KtProperty -> result.nameIdentifier?.startOffset ?: result.startOffset
                else -> result.startOffset
            }

            editor?.moveCaret(offset)
        }
    }
}

class LoopToCallChainIntention : AbstractLoopToCallChainIntention(
        lazy = false,
        text = "Replace with stdlib operations"
)

class LoopToLazyCallChainIntention : AbstractLoopToCallChainIntention(
        lazy = true,
        text = "Replace with stdlib operations with use of 'asSequence()'"
)

abstract class AbstractLoopToCallChainIntention(
        private val lazy: Boolean,
        text: String
) : SelfTargetingRangeIntention<KtForExpression>(
        KtForExpression::class.java,
        text
) {
    override fun applicabilityRange(element: KtForExpression): TextRange? {
        val match = match(element, lazy, false)
        text = if (match != null) "Replace with '${match.transformationMatch.buildPresentation()}'" else defaultText
        return if (match != null) element.forKeyword.textRange else null
    }

    private fun TransformationMatch.Result.buildPresentation(): String {
        return buildPresentation(sequenceTransformations + resultTransformation, null)
    }

    private fun buildPresentation(transformations: List<Transformation>, prevPresentation: String?): String {
        if (transformations.size > MAX) {
            if (transformations[0] is AsSequenceTransformation) {
                return buildPresentation(transformations.drop(1), transformations[0].presentation)
            }

            return buildPresentation(transformations.drop(transformations.size - MAX), prevPresentation?.let { it + ".." } ?: "..")
        }

        var result: String? = prevPresentation
        for (transformation in transformations) {
            result = transformation.buildPresentation(result)
        }
        return result!!
    }

    override fun applyTo(element: KtForExpression, editor: Editor?) {
        LoopToCallChainInspection.Fix(lazy).applyFix(element, editor)
    }

    companion object {
        const val MAX = 3
    }
}
