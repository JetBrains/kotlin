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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi

class KDocMissingDocumentationInspection(): AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
            KDocMissingDocumentationInspection(holder)

    private class KDocMissingDocumentationInspection(private val holder: ProblemsHolder): PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {

            if (element is KtNamedDeclaration) {
                val nameIdentifier = element.nameIdentifier
                val descriptor = element.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? MemberDescriptor
                if (nameIdentifier != null && descriptor?.visibility == Visibilities.PUBLIC) {
                    val hasDocumentation = element.docComment != null ||
                                           (descriptor as? CallableMemberDescriptor)?.overriddenDescriptors
                                                   ?.any { (it.source.getPsi() as? KtNamedDeclaration)?.docComment != null } ?: false
                    if (!hasDocumentation) {
                        holder.registerProblem(nameIdentifier, "Missing documentation", AddDocumentationFix())
                    }
                }
            }
        }
    }

    class AddDocumentationFix : LocalQuickFix {
        override fun getName(): String = "Add documentation"

        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val declaration = descriptor.psiElement.getParentOfType<KtNamedDeclaration>(true)
                              ?: throw IllegalStateException("Can't find declaration")
            declaration.addBefore(KDocElementFactory(project).createKDocFromText("/**  */"), declaration.firstChild)
            val editor = descriptor.psiElement.findExistingEditor()
            editor?.caretModel?.moveToOffset(declaration.startOffset + 4)
        }
    }
}
