/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

interface KotlinMangler<D : Any> {

    val String.hashMangle: Long

    fun D.isExported(compatibleMode: Boolean): Boolean
    fun D.mangleString(): String
    fun D.signatureString(): String
    fun D.fqnString(): String

    fun D.hashedMangle(): Long = mangleString().hashMangle
    fun D.signatureMangle(): Long = signatureString().hashMangle
    fun D.fqnMangle(): Long = fqnString().hashMangle

    fun D.isPlatformSpecificExport(): Boolean = false

    val manglerName: String

    interface DescriptorMangler : KotlinMangler<DeclarationDescriptor> {
        override val manglerName: String
            get() = "Descriptor"

        fun ClassDescriptor.mangleEnumEntryString(): String

        fun PropertyDescriptor.mangleFieldString(): String
    }

    interface IrMangler : KotlinMangler<IrDeclaration> {
        override val manglerName: String
            get() = "Ir"
    }
}
