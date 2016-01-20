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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import com.intellij.usageView.UsageViewLongNameLocation
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils

class KotlinElementDescriptionProvider : ElementDescriptionProvider {
    override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
        val targetElement = element.unwrapped

        fun elementKind() = when (targetElement) {
            is KtClass -> if (targetElement.isInterface()) "interface" else "class"
            is KtObjectDeclaration -> "object"
            is KtNamedFunction -> "function"
            is KtPrimaryConstructor, is KtSecondaryConstructor -> "constructor"
            is KtProperty -> if (targetElement.isLocal) "variable" else "property"
            is KtTypeParameter -> "type parameter"
            is KtParameter -> "parameter"
            is KtDestructuringDeclarationEntry -> "variable"
            else -> null
        }

        fun targetDescriptor(): DeclarationDescriptor? {
            val descriptor = (targetElement as KtDeclaration).descriptor ?: return null
            if (descriptor is ConstructorDescriptor) {
                return descriptor.containingDeclaration
            }
            return descriptor
        }

        if (targetElement !is PsiNamedElement || targetElement !is KtElement) return null

        return when(location) {
            is UsageViewLongNameLocation -> targetElement.getName()
            is RefactoringDescriptionLocation -> {
                val kind = elementKind() ?: return null
                val descriptor = targetDescriptor() ?: return null
                val desc =
                        if (location.includeParent() && targetElement !is KtTypeParameter && targetElement !is KtParameter) {
                            DescriptorUtils.getFqName(descriptor).asString()
                        }
                        else {
                            descriptor.name.asString()
                        }

                "$kind ${CommonRefactoringUtil.htmlEmphasize(desc)}"
            }
            is HighlightUsagesDescriptionLocation -> {
                val kind = elementKind() ?: return null
                val descriptor = targetDescriptor() ?: return null
                "$kind ${descriptor.name.asString()}"
            }
            else -> null
        }
    }
}
