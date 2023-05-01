/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.KotlinMangler

/**
 * A dummy signature composer that returns `null` for all declaration descriptors.
 */
class DescriptorSignatureComposerStub(override val mangler: KotlinMangler.DescriptorMangler) : IdSignatureComposer {
    override fun composeSignature(descriptor: DeclarationDescriptor): IdSignature? {
        return null
    }

    override fun composeEnumEntrySignature(descriptor: ClassDescriptor): IdSignature? {
        return null
    }

    override fun composeFieldSignature(descriptor: PropertyDescriptor): IdSignature? {
        return null
    }

    override fun composeAnonInitSignature(descriptor: ClassDescriptor): IdSignature? {
        return null
    }

    override fun withFileSignature(fileSignature: IdSignature.FileSignature, body: () -> Unit) {
        body()
    }
}
