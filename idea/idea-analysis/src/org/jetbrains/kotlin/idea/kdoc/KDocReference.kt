/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtDescriptorsBasedReference
import org.jetbrains.kotlin.idea.references.KtMultiReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KDocReferenceDescriptorsImpl(element: KDocName) : KDocReference(element), KtDescriptorsBasedReference {
    override fun isReferenceTo(element: PsiElement): Boolean =
        super<KtDescriptorsBasedReference>.isReferenceTo(element)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val declaration = element.getContainingDoc().getOwner() ?: return arrayListOf()
        val resolutionFacade = element.getResolutionFacade()
        val correctContext = resolutionFacade.analyze(declaration, BodyResolveMode.PARTIAL)
        val declarationDescriptor = correctContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return arrayListOf()

        val kdocLink = element.getStrictParentOfType<KDocLink>()!!
        return resolveKDocLink(
            correctContext,
            resolutionFacade,
            declarationDescriptor,
            kdocLink.getTagIfSubject(),
            element.getQualifiedName()
        )
    }

    override fun handleElementRename(newElementName: String): PsiElement? {
        val textRange = element.getNameTextRange()
        val newText = textRange.replace(element.text, newElementName)
        val newLink = KDocElementFactory(element.project).createNameFromText(newText)
        return element.replace(newLink)
    }
}
