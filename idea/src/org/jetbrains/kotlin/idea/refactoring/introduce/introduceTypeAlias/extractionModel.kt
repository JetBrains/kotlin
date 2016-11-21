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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class TypeReferenceInfo(val reference: KtTypeReference, val type: KotlinType)

internal var KtTypeReference.resolveInfo : TypeReferenceInfo? by CopyableUserDataProperty(Key.create("RESOLVE_INFO"))

class IntroduceTypeAliasData(
        val originalTypeElement: KtElement,
        val targetSibling: PsiElement,
        val extractTypeConstructor: Boolean = false
) : Disposable {
    val resolutionFacade = originalTypeElement.getResolutionFacade()
    val bindingContext = resolutionFacade.analyze(originalTypeElement, BodyResolveMode.PARTIAL)

    init {
        markReferences()
    }

    private fun markReferences() {
        val visitor = object : KtTreeVisitorVoid() {
            override fun visitTypeReference(typeReference: KtTypeReference) {
                val typeElement = typeReference.typeElement ?: return

                val kotlinType = bindingContext[BindingContext.ABBREVIATED_TYPE, typeReference] ?:
                                 bindingContext[BindingContext.TYPE, typeReference] ?:
                                 return
                typeReference.resolveInfo = TypeReferenceInfo(typeReference, kotlinType)

                typeElement.typeArgumentsAsTypes.forEach { it.accept(this) }
            }
        }
        (originalTypeElement.parent as? KtTypeReference ?: originalTypeElement).accept(visitor)
    }

    override fun dispose() {
        if (!originalTypeElement.isValid) return
        originalTypeElement.forEachDescendantOfType<KtTypeReference> { it.resolveInfo = null }
    }
}

data class TypeParameter(val name: String, val typeReferenceInfos: Collection<TypeReferenceInfo>)

data class IntroduceTypeAliasDescriptor(
        val originalData: IntroduceTypeAliasData,
        val name: String,
        val visibility: KtModifierKeywordToken?,
        val typeParameters: List<TypeParameter>
)

data class IntroduceTypeAliasDescriptorWithConflicts(
        val descriptor: IntroduceTypeAliasDescriptor,
        val conflicts: MultiMap<PsiElement, String>
)