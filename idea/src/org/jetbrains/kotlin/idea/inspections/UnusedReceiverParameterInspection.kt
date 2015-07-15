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

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.getThisReceiverOwner
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isOverridable
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import kotlin.properties.Delegates

public class UnusedReceiverParameterInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JetVisitorVoid() {
            private fun check(callableDeclaration: JetCallableDeclaration) {
                val receiverTypeReference = callableDeclaration.getReceiverTypeReference()
                if (receiverTypeReference == null) return
                if (callableDeclaration.isOverridable() || callableDeclaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return

                if (callableDeclaration is JetProperty && callableDeclaration.getAccessors().isEmpty()) return
                if (callableDeclaration is JetNamedFunction && !callableDeclaration.hasBody()) return

                val callable = callableDeclaration.descriptor

                var used = false
                callableDeclaration.acceptChildren(object : JetVisitorVoid() {
                    override fun visitJetElement(element: JetElement) {
                        if (used) return
                        element.acceptChildren(this)

                        val bindingContext = element.analyze()
                        val resolvedCall = element.getResolvedCall(bindingContext) ?: return

                        if (resolvedCall.getDispatchReceiver().getThisReceiverOwner(bindingContext) == callable ||
                            resolvedCall.getExtensionReceiver().getThisReceiverOwner(bindingContext) == callable) {
                            used = true
                        }
                        else if ((resolvedCall.getCandidateDescriptor() as? ReceiverParameterDescriptor)?.getContainingDeclaration() == callable) {
                            used = true
                        }
                    }
                })

                if (!used) {
                    holder.registerProblem(
                            receiverTypeReference,
                            JetBundle.message("unused.receiver.parameter"),
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                            MyQuickFix(callableDeclaration)
                    )
                }
            }

            override fun visitNamedFunction(function: JetNamedFunction) {
                check(function)
            }

            override fun visitProperty(property: JetProperty) {
                check(property)
            }
        }
    }

    private class MyQuickFix(val declaration: JetCallableDeclaration): LocalQuickFix {
        override fun getName(): String {
            return JetBundle.message("unused.receiver.parameter.remove")
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            declaration.setReceiverTypeReference(null)
        }

        override fun getFamilyName(): String = getName()
    }
}
