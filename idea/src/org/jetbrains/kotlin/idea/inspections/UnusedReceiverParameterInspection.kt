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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeSignatureConfiguration
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.modify
import org.jetbrains.kotlin.idea.refactoring.changeSignature.runChangeSignature
import org.jetbrains.kotlin.idea.refactoring.explicateAsTextForReceiver
import org.jetbrains.kotlin.idea.refactoring.getThisLabelName
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getThisReceiverOwner
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class UnusedReceiverParameterInspection : AbstractKotlinInspection() {
    override val suppressionKey: String get() = "unused"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            private fun check(callableDeclaration: KtCallableDeclaration) {
                val receiverTypeReference = callableDeclaration.receiverTypeReference
                if (receiverTypeReference == null || receiverTypeReference.textRange.isEmpty) return

                val context = receiverTypeReference.analyze()
                val receiverType = context[BindingContext.TYPE, receiverTypeReference] ?: return
                val receiverTypeDeclaration = receiverType.constructor.declarationDescriptor
                if (DescriptorUtils.isCompanionObject(receiverTypeDeclaration)) return

                if (callableDeclaration.isOverridable() ||
                    callableDeclaration.hasModifier(KtTokens.OVERRIDE_KEYWORD) ||
                    callableDeclaration.hasModifier(KtTokens.OPERATOR_KEYWORD) ||
                    callableDeclaration.hasModifier(KtTokens.INFIX_KEYWORD) ||
                    callableDeclaration.hasActualModifier()
                ) return

                if (callableDeclaration is KtProperty && callableDeclaration.accessors.isEmpty()) return
                if (callableDeclaration is KtNamedFunction && !callableDeclaration.hasBody()) return

                val callable = callableDeclaration.descriptor

                if (callable != null && MainFunctionDetector.isMain(callable)) return

                val containingDeclaration = callable?.containingDeclaration
                if (containingDeclaration != null && containingDeclaration == receiverTypeDeclaration) {
                    val thisLabelName = containingDeclaration.getThisLabelName()
                    if (!callableDeclaration.anyDescendantOfType<KtThisExpression> { it.getLabelName() == thisLabelName }) {
                        registerProblem(receiverTypeReference, true)
                    }
                    return
                }

                var used = false
                callableDeclaration.acceptChildren(object : KtVisitorVoid() {
                    override fun visitKtElement(element: KtElement) {
                        if (used) return
                        element.acceptChildren(this)

                        val bindingContext = element.analyze()
                        val resolvedCall = element.getResolvedCall(bindingContext) ?: return

                        if (isUsageOfReceiver(resolvedCall, bindingContext)) {
                            used = true
                        } else if (resolvedCall is VariableAsFunctionResolvedCall
                            && isUsageOfReceiver(resolvedCall.variableCall, bindingContext)
                        ) {
                            used = true
                        }
                    }

                    private fun isUsageOfReceiver(resolvedCall: ResolvedCall<*>, bindingContext: BindingContext): Boolean {
                        // As receiver of call
                        if (resolvedCall.dispatchReceiver.getThisReceiverOwner(bindingContext) == callable ||
                            resolvedCall.extensionReceiver.getThisReceiverOwner(bindingContext) == callable
                        ) {
                            return true
                        }
                        // As explicit "this"
                        if ((resolvedCall.candidateDescriptor as? ReceiverParameterDescriptor)?.containingDeclaration == callable) {
                            return true
                        }
                        return false
                    }
                })

                if (!used) registerProblem(receiverTypeReference)
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                check(function)
            }

            override fun visitProperty(property: KtProperty) {
                check(property)
            }

            private fun registerProblem(receiverTypeReference: KtTypeReference, inSameClass: Boolean = false) {
                holder.registerProblem(
                    receiverTypeReference,
                    KotlinBundle.message("unused.receiver.parameter"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    MyQuickFix(inSameClass)
                )
            }
        }
    }

    private class MyQuickFix(private val inSameClass: Boolean) : LocalQuickFix {
        override fun getName(): String = KotlinBundle.message("unused.receiver.parameter.remove")

        private fun configureChangeSignature() = object : KotlinChangeSignatureConfiguration {
            override fun performSilently(affectedFunctions: Collection<PsiElement>) = true
            override fun configure(originalDescriptor: KotlinMethodDescriptor) = originalDescriptor.modify { it.removeParameter(0) }
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            if (!FileModificationService.getInstance().preparePsiElementForWrite(element)) return

            val function = element.parent as? KtCallableDeclaration ?: return
            val callableDescriptor = function.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? CallableDescriptor ?: return

            if (inSameClass) {
                runWriteAction {
                    val explicateAsTextForReceiver = callableDescriptor.explicateAsTextForReceiver()
                    function.forEachDescendantOfType<KtThisExpression> {
                        if (it.text == explicateAsTextForReceiver) it.labelQualifier?.delete()
                    }
                    function.setReceiverTypeReference(null)
                }
            } else {
                runChangeSignature(project, callableDescriptor, configureChangeSignature(), element, name)
            }
        }

        override fun getFamilyName(): String = name

        override fun startInWriteAction() = false
    }
}
