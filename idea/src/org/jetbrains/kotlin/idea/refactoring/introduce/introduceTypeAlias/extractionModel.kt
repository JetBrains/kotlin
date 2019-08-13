/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.CopyablePsiUserDataProperty
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class TypeReferenceInfo(val reference: KtTypeReference, val type: KotlinType)

internal var KtTypeReference.resolveInfo: TypeReferenceInfo? by CopyablePsiUserDataProperty(Key.create("RESOLVE_INFO"))

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

                val kotlinType = bindingContext[BindingContext.ABBREVIATED_TYPE, typeReference]
                    ?: bindingContext[BindingContext.TYPE, typeReference]
                    ?: return
                typeReference.resolveInfo = TypeReferenceInfo(typeReference, kotlinType)

                typeElement.typeArgumentsAsTypes.forEach { it?.accept(this) }
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