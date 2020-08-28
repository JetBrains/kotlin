/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.resolve.BindingContext

class KtDestructuringDeclarationReferenceDescriptorsImpl(
    element: KtDestructuringDeclarationEntry
) : KtDestructuringDeclarationReference(element), KtDescriptorsBasedReference {
    override fun isReferenceTo(element: PsiElement): Boolean =
        super<KtDescriptorsBasedReference>.isReferenceTo(element)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return listOfNotNull(context[BindingContext.COMPONENT_RESOLVED_CALL, element]?.candidateDescriptor)
    }

    override fun getRangeInElement() = TextRange(0, element.textLength)

    override fun canRename(): Boolean {
        val bindingContext = expression.analyze() //TODO: should it use full body resolve?
        return resolveToDescriptors(bindingContext).all {
            it is CallableMemberDescriptor && it.kind == CallableMemberDescriptor.Kind.SYNTHESIZED
        }
    }
}
