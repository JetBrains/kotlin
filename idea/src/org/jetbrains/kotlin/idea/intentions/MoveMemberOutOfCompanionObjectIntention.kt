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
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.getUsageContext
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.util.findCallableMemberBySignature

class MoveMemberOutOfCompanionObjectIntention : SelfTargetingRangeIntention<KtNamedDeclaration>(KtNamedDeclaration::class.java,
                                                                                                "Move out of companion object") {
    override fun startInWriteAction() = false

    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (element !is KtNamedFunction && element !is KtProperty && element !is KtClassOrObject) return null
        val container = element.containingClassOrObject
        if (!(container is KtObjectDeclaration && container.isCompanion())) return null
        if (container.containingClassOrObject == null) return null
        return element.nameIdentifier?.textRange
    }

    override fun applyTo(element: KtNamedDeclaration, editor: Editor?) {
        val project = element.project

        val companionObject = element.containingClassOrObject!!
        val targetClass = companionObject.containingClassOrObject!!

        fun deleteCompanionIfEmpty() {
            if (companionObject.declarations.isEmpty()) {
                companionObject.delete()
            }
        }

        if (element is KtClassOrObject) {
            val moveDescriptor = MoveDeclarationsDescriptor(project,
                                                            listOf(element),
                                                            KotlinMoveTargetForExistingElement(targetClass),
                                                            MoveDeclarationsDelegate.NestedClass())
            runWriteAction {
                MoveKotlinDeclarationsProcessor(moveDescriptor).run()
                deleteCompanionIfEmpty()
            }
            return
        }

        val targetClassDescriptor = runReadAction { targetClass.unsafeResolveToDescriptor() as ClassDescriptor }

        val conflicts = MultiMap<PsiElement, String>()

        val refsRequiringClassInstance = project.runSynchronouslyWithProgress("Searching for ${element.name}", true) {
            runReadAction {
                ReferencesSearch
                        .search(element)
                        .mapNotNull { it.element }
                        .filter {
                            if (it !is KtElement) return@filter true
                            val resolvedCall = it.getResolvedCall(it.analyzeFully()) ?: return@filter false
                            val dispatchReceiver = resolvedCall.dispatchReceiver ?: return@filter false
                            if (dispatchReceiver !is ImplicitClassReceiver) return@filter true
                            it.parents
                                    .filterIsInstance<KtClassOrObject>()
                                    .none {
                                        val classDescriptor = it.resolveToDescriptorIfAny() as? ClassDescriptor
                                        if (classDescriptor != null && classDescriptor.isSubclassOf(targetClassDescriptor)) return@none true
                                        if (it.isTopLevel() || it is KtObjectDeclaration || (it is KtClass && !it.isInner())) return@filter true
                                        false
                                    }
                        }
            }
        } ?: return

        for (refElement in refsRequiringClassInstance) {
            val context = refElement.getUsageContext()
            val message = "'${refElement.text}' in ${RefactoringUIUtil.getDescription(context, false)} will require class instance"
            conflicts.putValue(refElement, message)
        }

        runReadAction {
            val callableDescriptor = element.unsafeResolveToDescriptor() as CallableMemberDescriptor
            targetClassDescriptor.findCallableMemberBySignature(callableDescriptor)?.let {
                DescriptorToSourceUtilsIde.getAnyDeclaration(project, it)
            }?.let {
                conflicts.putValue(it, "Class '${targetClass.name}' already contains ${RefactoringUIUtil.getDescription(it, false)}")
            }
        }

        project.checkConflictsInteractively(conflicts) {
            runWriteAction {
                Mover.Default(element, targetClass)
                deleteCompanionIfEmpty()
            }
        }
    }
}