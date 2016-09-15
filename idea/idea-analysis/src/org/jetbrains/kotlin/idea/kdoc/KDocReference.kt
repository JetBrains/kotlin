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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.references.KtMultiReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

class KDocReference(element: KDocName): KtMultiReference<KDocName>(element) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val declaration = element.getContainingDoc().getOwner() ?: return arrayListOf()
        val declarationDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return arrayListOf()

        val kdocLink = element.getStrictParentOfType<KDocLink>()!!
        return resolveKDocLink(context,
                               element.getResolutionFacade(),
                               declarationDescriptor,
                               kdocLink.getTagIfSubject(),
                               element.getQualifiedName())
    }

    override fun getRangeInElement(): TextRange = element.getNameTextRange()

    override fun canRename(): Boolean = true

    override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element

    override fun handleElementRename(newElementName: String?): PsiElement? {
        val textRange = element.getNameTextRange()
        val newText = textRange.replace(element.text, newElementName!!)
        val newLink = KDocElementFactory(element.project).createNameFromText(newText)
        return element.replace(newLink)
    }

    override fun getCanonicalText(): String = element.getNameText()

    override val resolvesByNames: Collection<String> get() = listOf(element.getNameText())
}
