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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.util.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.refactoring.getUsageContext
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.hasJvmFieldAnnotation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.util.findCallableMemberBySignature

class MoveMemberOutOfCompanionObjectIntention : MoveMemberOutOfObjectIntention(KotlinBundle.lazyMessage("move.out.of.companion.object")) {
    override fun addConflicts(element: KtNamedDeclaration, conflicts: MultiMap<PsiElement, String>) {
        val targetClass = element.containingClassOrObject?.containingClassOrObject ?: return
        val targetClassDescriptor = runReadAction { targetClass.unsafeResolveToDescriptor() as ClassDescriptor }

        val refsRequiringClassInstance =
            element.project.runSynchronouslyWithProgress(KotlinBundle.message("searching.for.0", element.name.toString()), true) {
                runReadAction {
                    ReferencesSearch
                        .search(element)
                        .mapNotNull { it.element }
                        .filter {
                            if (it !is KtElement) return@filter true
                            val resolvedCall = it.resolveToCall() ?: return@filter false
                            val dispatchReceiver = resolvedCall.dispatchReceiver ?: return@filter false
                            if (dispatchReceiver !is ImplicitClassReceiver) return@filter true
                            it.parents
                                .filterIsInstance<KtClassOrObject>()
                                .none {
                                    val classDescriptor = it.resolveToDescriptorIfAny()
                                    if (classDescriptor != null && classDescriptor.isSubclassOf(targetClassDescriptor)) return@none true
                                    if (it.isTopLevel() || it is KtObjectDeclaration || (it is KtClass && !it.isInner())) return@filter true
                                    false
                                }
                        }
                }
            } ?: return

        for (refElement in refsRequiringClassInstance) {
            val context = refElement.getUsageContext()
            val message = KotlinBundle.message(
                "0.in.1.will.require.class.instance",
                refElement.text,
                RefactoringUIUtil.getDescription(context, false)
            )
            conflicts.putValue(refElement, message)
        }

        runReadAction {
            val callableDescriptor = element.unsafeResolveToDescriptor() as CallableMemberDescriptor
            targetClassDescriptor.findCallableMemberBySignature(callableDescriptor)?.let {
                DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, it)
            }?.let {
                conflicts.putValue(
                    it,
                    KotlinBundle.message(
                        "class.0.already.contains.1",
                        targetClass.name.toString(),
                        RefactoringUIUtil.getDescription(it, false)
                    )
                )
            }
        }

    }

    override fun getDestination(element: KtNamedDeclaration) = element.containingClassOrObject!!.containingClassOrObject!!

    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (element !is KtNamedFunction && element !is KtProperty && element !is KtClassOrObject) return null
        val container = element.containingClassOrObject
        if (!(container is KtObjectDeclaration && container.isCompanion())) return null
        val containingClassOrObject = container.containingClassOrObject ?: return null
        if (containingClassOrObject.isInterfaceClass() && element.hasJvmFieldAnnotation()) return null
        return element.nameIdentifier?.textRange
    }

}