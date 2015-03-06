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

package org.jetbrains.kotlin.idea.kdoc

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

object KDocFinder {
    fun findKDoc(declaration: DeclarationDescriptor): KDocTag? {
        if (declaration is DeclarationDescriptorWithSource) {
            val psiDeclaration = (declaration.getSource() as? PsiSourceElement)?.psi?.getNavigationElement()
            if (psiDeclaration is JetDeclaration) {
                val kdoc = psiDeclaration.getDocComment()
                if (kdoc != null) {
                    if (declaration is ConstructorDescriptor) {
                        // ConstructorDescriptor resolves to the same JetDeclaration
                        val constructorSection = kdoc.findSectionByTag(KDocKnownTag.CONSTRUCTOR)
                        if (constructorSection != null) {
                            return constructorSection
                        }
                    }
                    return kdoc.getDefaultSection()
                }
            }
        }

        if (declaration is PropertyDescriptor) {
            val containingClassDescriptor = declaration.getContainingDeclaration() as? ClassDescriptor
            if (containingClassDescriptor != null) {
                val classKDoc = findKDoc(containingClassDescriptor)?.getParentOfType<KDoc>(false)
                if (classKDoc != null) {
                    val propertySection = classKDoc.findSectionByTag(KDocKnownTag.PROPERTY,
                                                                     declaration.getName().asString())
                    if (propertySection != null) {
                        return propertySection
                    }
                }
            }
        }

        if (declaration is CallableDescriptor) {
            for (baseDescriptor in declaration.getOverriddenDescriptors()) {
                val baseKDoc = findKDoc(baseDescriptor.getOriginal())
                if (baseKDoc != null) {
                    return baseKDoc
                }
            }
        }

        return null
    }
}
