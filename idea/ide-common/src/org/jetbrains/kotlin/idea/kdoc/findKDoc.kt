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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

fun DeclarationDescriptor.findKDoc(): KDocTag? {
    if (this is DeclarationDescriptorWithSource) {
        var psiDeclaration = (this.source as? PsiSourceElement)?.psi?.navigationElement
        // KDoc for primary constructor is located inside of its class KDoc
        if (psiDeclaration is KtPrimaryConstructor) {
            psiDeclaration = psiDeclaration.getContainingClassOrObject()
        }

        if (psiDeclaration is KtDeclaration) {
            val kdoc = psiDeclaration.docComment
            if (kdoc != null) {
                if (this is ConstructorDescriptor) {
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

    if (this is PropertyDescriptor) {
        val containingClassDescriptor = this.containingDeclaration as? ClassDescriptor
        if (containingClassDescriptor != null) {
            val classKDoc = containingClassDescriptor.findKDoc()?.getParentOfType<KDoc>(false)
            if (classKDoc != null) {
                val propertySection = classKDoc.findSectionByTag(KDocKnownTag.PROPERTY,
                                                                 getName().asString())
                if (propertySection != null) {
                    return propertySection
                }
            }
        }
    }

    if (this is CallableDescriptor) {
        for (baseDescriptor in this.overriddenDescriptors) {
            val baseKDoc = baseDescriptor.original.findKDoc()
            if (baseKDoc != null) {
                return baseKDoc
            }
        }
    }

    return null
}

