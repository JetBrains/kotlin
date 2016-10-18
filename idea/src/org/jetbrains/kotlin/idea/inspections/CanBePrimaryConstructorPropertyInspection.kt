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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class CanBePrimaryConstructorPropertyInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                if (property.isLocal) return
                if (property.getter != null || property.setter != null || property.delegate != null) return
                val assigned = property.initializer as? KtReferenceExpression ?: return

                val context = property.analyzeFully()
                val assignedDescriptor = context.get(BindingContext.REFERENCE_TARGET, assigned) as? ValueParameterDescriptor ?: return
                // to prevent some exotic situations
                if (!assignedDescriptor.annotations.isEmpty() || !assigned.getAnnotationEntries().isEmpty()) return

                val containingConstructor = assignedDescriptor.containingDeclaration as? ClassConstructorDescriptor ?: return
                if (containingConstructor.containingDeclaration.isData) return

                val propertyTypeReference = property.typeReference
                val propertyType = context.get(BindingContext.TYPE, propertyTypeReference)
                if (propertyType != null && propertyType != assignedDescriptor.type) return

                val nameIdentifier = property.nameIdentifier ?: return
                if (nameIdentifier.text != assignedDescriptor.name.asString()) return

                val assignedParameter = DescriptorToSourceUtils.descriptorToDeclaration(assignedDescriptor) as? KtParameter ?: return
                if (property.containingClassOrObject !== assignedParameter.containingClassOrObject) return

                holder.registerProblem(holder.manager.createProblemDescriptor(
                        nameIdentifier,
                        nameIdentifier,
                        "Property is explicitly assigned to parameter ${assignedDescriptor.name}, can be declared directly in constructor",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOnTheFly,
                        MakeConstructorPropertyFix(property, assignedParameter)
                ))
            }
        }
    }

    class MakeConstructorPropertyFix(val original: KtProperty, val parameter: KtParameter) : LocalQuickFix {
        override fun getName() = "Move to constructor"

        override fun getFamilyName() = "Move to constructor"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return

            val commentSaver = CommentSaver(original)
            val isVar = original.isVar
            val modifiers = original.modifierList?.text
            val factory = KtPsiFactory(project)
            val valOrVar = if (isVar) factory.createVarKeyword() else factory.createValKeyword()
            parameter.addBefore(valOrVar, parameter.nameIdentifier)
            if (modifiers != null) {
                val newModifiers = factory.createModifierList(modifiers)
                parameter.addBefore(newModifiers, parameter.valOrVarKeyword)
            }
            original.delete()
            commentSaver.restore(parameter)
        }
    }
}