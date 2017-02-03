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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.InspectionsBundle
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateEqualsAndHashcodeAction
import org.jetbrains.kotlin.idea.actions.generate.findDeclaredEquals
import org.jetbrains.kotlin.idea.actions.generate.findDeclaredHashCode
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.source.getPsi

object DeleteEqualsAndHashCodeFix : LocalQuickFix {
    override fun getName() = "Delete equals()/hashCode()"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
        val objectDeclaration = descriptor.psiElement.getStrictParentOfType<KtObjectDeclaration>() ?: return
        val classDescriptor = objectDeclaration.resolveToDescriptorIfAny() as? ClassDescriptor ?: return
        classDescriptor.findDeclaredEquals(false)?.source?.getPsi()?.delete()
        classDescriptor.findDeclaredHashCode(false)?.source?.getPsi()?.delete()
    }
}

sealed class GenerateEqualsOrHashCodeFix : LocalQuickFix {
    object Equals : GenerateEqualsOrHashCodeFix() {
        override fun getName() = "Generate 'equals()'"
    }

    object HashCode : GenerateEqualsOrHashCodeFix() {
        override fun getName() = "Generate 'hashCode()'"
    }

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
        KotlinGenerateEqualsAndHashcodeAction().doInvoke(project, null, descriptor.psiElement.parent as KtClass)
    }
}

class EqualsOrHashCodeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object: KtVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                val nameIdentifier = classOrObject.nameIdentifier ?: return
                val classDescriptor = classOrObject.resolveToDescriptorIfAny() as? ClassDescriptor ?: return
                val hasEquals = classDescriptor.findDeclaredEquals(false) != null
                val hasHashCode = classDescriptor.findDeclaredHashCode(false) != null
                if (!hasEquals && !hasHashCode) return

                when (classDescriptor.kind) {
                    ClassKind.OBJECT -> {
                        if (classOrObject.superTypeListEntries.isNotEmpty()) return
                        holder.registerProblem(nameIdentifier, "equals()/hashCode() in object declaration", DeleteEqualsAndHashCodeFix)
                    }
                    ClassKind.CLASS -> {
                        if (hasEquals && hasHashCode) return
                        val description = InspectionsBundle.message(
                                "inspection.equals.hashcode.only.one.defined.problem.descriptor",
                                if (hasEquals) "<code>equals()</code>" else "<code>hashCode()</code>",
                                if (hasEquals) "<code>hashCode()</code>" else "<code>equals()</code>"
                        )
                        holder.registerProblem(
                                nameIdentifier,
                                description,
                                if (hasEquals) GenerateEqualsOrHashCodeFix.HashCode else GenerateEqualsOrHashCodeFix.Equals
                        )
                    }
                    else -> return
                }
            }
        }
    }
}