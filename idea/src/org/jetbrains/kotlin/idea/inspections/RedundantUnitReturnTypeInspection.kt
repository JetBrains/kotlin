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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeIntention
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

class RedundantUnitReturnTypeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                if (function.containingFile is KtCodeFragment) return
                val typeElement = function.typeReference?.typeElement ?: return
                val context = function.analyze(BodyResolveMode.PARTIAL)
                val descriptor = context[BindingContext.FUNCTION, function] ?: return
                if (descriptor.returnType?.isUnit() == true) {
                    if (!function.hasBlockBody()) {
                        val bodyExpression = function.bodyExpression
                        if (bodyExpression != null) {
                            val bodyContext = bodyExpression.analyze(BodyResolveMode.PARTIAL)
                            if (bodyContext.getType(bodyExpression)?.isNothing() == true) return
                            val resolvedCall = bodyExpression.getResolvedCall(bodyContext)
                            if (resolvedCall != null) {
                                if (resolvedCall.candidateDescriptor.returnType?.isUnit() != true) return
                            }
                        }
                    }

                    holder.registerProblem(typeElement,
                                           "Redundant 'Unit' return type",
                                           ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                           IntentionWrapper(RemoveExplicitTypeIntention(), function.containingKtFile))
                }
            }
        }
    }
}