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

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.ReplaceWithDotCallFix
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils

class UselessCallOnNotNullInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
                super.visitQualifiedExpression(expression)
                val selector = expression.selectorExpression as? KtCallExpression ?: return
                val calleeExpression = selector.calleeExpression ?: return
                if (calleeExpression.text !in names) return

                val context = expression.analyze(BodyResolveMode.PARTIAL)
                val resolvedCall = expression.getResolvedCall(context) ?: return
                val conversion = fqNames[resolvedCall.resultingDescriptor.fqNameOrNull()?.asString()] ?: return
                val newName = conversion.replacementName

                val safeExpression = expression as? KtSafeQualifiedExpression
                val notNullType = expression.receiverExpression.getType(context)?.let { TypeUtils.isNullableType(it) } == false
                if (newName != null && (notNullType || safeExpression != null)) {
                    val descriptor = holder.manager.createProblemDescriptor(
                            expression,
                            TextRange(expression.operationTokenNode.startOffset - expression.startOffset,
                                      calleeExpression.endOffset - expression.startOffset),
                            "Call on not-null type may be reduced",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            isOnTheFly,
                            RenameUselessCallFix(newName)
                    )
                    holder.registerProblem(descriptor)
                }
                else if (notNullType) {
                    val descriptor = holder.manager.createProblemDescriptor(
                            expression,
                            TextRange(expression.operationTokenNode.startOffset - expression.startOffset,
                                      calleeExpression.endOffset - expression.startOffset),
                            "Useless call on not-null type",
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                            isOnTheFly,
                            RemoveUselessCallFix()
                    )
                    holder.registerProblem(descriptor)
                }
                else if (safeExpression != null) {
                    holder.registerProblem(
                            safeExpression.operationTokenNode.psi,
                            "This call is useless with ?.",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            IntentionWrapper(ReplaceWithDotCallFix(safeExpression), safeExpression.containingKtFile)
                    )
                }
            }
        }
    }

    companion object {
        private data class Conversion(val replacementName: String? = null)

        private val deleteConversion = Conversion()

        private val fqNames = mapOf("kotlin.collections.orEmpty" to deleteConversion,
                                    "kotlin.text.orEmpty" to deleteConversion,
                                    "kotlin.text.isNullOrEmpty" to Conversion("isEmpty"),
                                    "kotlin.text.isNullOrBlank" to Conversion("isBlank"))

        private val names = fqNames.keys.mapTo(mutableSetOf()) { fqName -> fqName.takeLastWhile { it != '.' } }
    }
}

