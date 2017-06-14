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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.AddNamesToCallArgumentsIntention
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class CopyWithoutNamedArgumentsInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                val reference = expression.referenceExpression() as? KtNameReferenceExpression ?: return
                if (reference.getReferencedNameAsName() != DataClassDescriptorResolver.COPY_METHOD_NAME) return
                if (expression.valueArguments.all { it.isNamed() }) return

                val context = expression.analyze(BodyResolveMode.PARTIAL)
                val call = expression.getResolvedCall(context) ?: return
                val receiver = call.dispatchReceiver?.type?.constructor?.declarationDescriptor as? ClassDescriptor ?: return

                if (!receiver.isData) return
                if (call.candidateDescriptor != context[BindingContext.DATA_CLASS_COPY_FUNCTION, receiver]) return

                holder.registerProblem(
                        expression.calleeExpression ?: return,
                        "'copy' method of data class is called without named arguments",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        IntentionWrapper(AddNamesToCallArgumentsIntention(), expression.containingKtFile)
                )
            }
        }
    }

}