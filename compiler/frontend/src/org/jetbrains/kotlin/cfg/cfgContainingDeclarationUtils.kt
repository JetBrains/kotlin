/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cfg

import com.intellij.psi.util.PsiTreeUtil.getParentOfType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR

val KtElement.containingDeclarationForPseudocode: KtDeclaration?
    get() = getParentOfType(this, KtDeclarationWithBody::class.java, KtClassOrObject::class.java, KtScript::class.java)
        ?: getNonStrictParentOfType<KtProperty>()

// Should return KtDeclarationWithBody, KtClassOrObject, KtClassInitializer or KtScriptInitializer
fun KtElement.getElementParentDeclaration(): KtDeclaration? =
    getParentOfType(this, KtDeclarationWithBody::class.java, KtClassOrObject::class.java, KtClassInitializer::class.java,
        KtScriptInitializer::class.java)

fun KtDeclaration?.getDeclarationDescriptorIncludingConstructors(context: BindingContext): DeclarationDescriptor? {
    val descriptor = context.get(DECLARATION_TO_DESCRIPTOR, (this as? KtClassInitializer)?.containingDeclaration ?: this)
    return if (descriptor is ClassDescriptor && this is KtClassInitializer) {
        // For a class primary constructor, we cannot directly get ConstructorDescriptor by KtClassInitializer,
        // so we have to do additional conversion: KtClassInitializer -> KtClassOrObject -> ClassDescriptor -> ConstructorDescriptor
        descriptor.unsubstitutedPrimaryConstructor
            ?: (descriptor as? ClassDescriptorWithResolutionScopes)?.scopeForInitializerResolution?.ownerDescriptor
    } else {
        descriptor
    }
}
