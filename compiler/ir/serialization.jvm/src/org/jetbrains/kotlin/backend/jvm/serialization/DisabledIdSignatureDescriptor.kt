/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler

object DisabledDescriptorMangler : KotlinMangler.DescriptorMangler {
    override val String.hashMangle: Long
        get() = error("Should not be called")

    override fun DeclarationDescriptor.isExported(compatibleMode: Boolean): Boolean =
        error("Should not be called")

    override fun DeclarationDescriptor.mangleString(compatibleMode: Boolean): String =
        error("Should not be called")

    override fun DeclarationDescriptor.signatureString(compatibleMode: Boolean): String =
        error("Should not be called")

    override fun DeclarationDescriptor.fqnString(compatibleMode: Boolean): String =
        error("Should not be called")

    override fun ClassDescriptor.mangleEnumEntryString(compatibleMode: Boolean): String =
        error("Should not be called")

    override fun PropertyDescriptor.mangleFieldString(compatibleMode: Boolean): String =
        error("Should not be called")
}

object DisabledIdSignatureDescriptor : IdSignatureDescriptor(DisabledDescriptorMangler) {
    override fun composeSignature(descriptor: DeclarationDescriptor): IdSignature? = null

    override fun composeEnumEntrySignature(descriptor: ClassDescriptor): IdSignature? = null

    override fun composeFieldSignature(descriptor: PropertyDescriptor): IdSignature? = null

    override fun composeAnonInitSignature(descriptor: ClassDescriptor): IdSignature? = null

    override fun createSignatureBuilder(type: SpecialDeclarationType): DescriptorBasedSignatureBuilder =
        error("Should not be called")
}
