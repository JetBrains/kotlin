/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.findUsages

import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import com.intellij.usageView.UsageViewLongNameLocation
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.asJava.unwrapped
import org.jetbrains.jet.plugin.search.usagesSearch.descriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import com.intellij.refactoring.util.CommonRefactoringUtil

public class JetElementDescriptionProvider : ElementDescriptionProvider {
    public override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
        val targetElement = element.unwrapped

        fun elementKind() = when (targetElement) {
            is JetClass -> if (targetElement.isTrait()) "trait" else "class"
            is JetObjectDeclaration -> "object"
            is JetNamedFunction -> "function"
            is JetProperty -> "property"
            is JetTypeParameter -> "type parameter"
            is JetParameter -> "parameter"
            else -> null
        }

        if (targetElement !is PsiNamedElement || targetElement !is JetElement) return null

        val name = (targetElement as PsiNamedElement).getName()

        return when(location) {
            is UsageViewLongNameLocation ->
                name
            is RefactoringDescriptionLocation -> {
                val kind = elementKind()
                if (kind != null) {
                    val descriptor = (targetElement as JetDeclaration).descriptor
                    if (descriptor != null) {
                        val desc = if (location.includeParent() && targetElement !is JetTypeParameter && targetElement !is JetParameter) {
                            DescriptorUtils.getFqName(descriptor).asString()
                        }
                        else descriptor.getName().asString()

                        "$kind ${CommonRefactoringUtil.htmlEmphasize(desc)}"
                    }
                    else null
                }
                else null
            }
            else -> null
        }
    }
}
