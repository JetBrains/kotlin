/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

interface AbsentDescriptorHandler {
    fun diagnoseDescriptorNotFound(declaration: KtDeclaration): DeclarationDescriptor
}

class BasicAbsentDescriptorHandler : AbsentDescriptorHandler {
    override fun diagnoseDescriptorNotFound(declaration: KtDeclaration) = throw NoDescriptorForDeclarationException(declaration)
}

class NoDescriptorForDeclarationException @JvmOverloads constructor(declaration: KtDeclaration, additionalDetails: String? = null) :
    KotlinExceptionWithAttachments(
        "Descriptor wasn't found for declaration $declaration"
                + (additionalDetails?.let { "\n---------------------------------------------------\n$it" } ?: "")
    ) {
    init {
        withAttachment("declaration.kt", declaration.getElementTextWithContext())
    }
}