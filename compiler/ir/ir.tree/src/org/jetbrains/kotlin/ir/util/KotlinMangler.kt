/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.types.KotlinType

interface KotlinMangler<D : Any> {

    val String.hashMangle: Long

    fun D.isExported(compatibleMode: Boolean): Boolean
//    fun D.isExported(): Boolean
    fun D.mangleString(localNameResolver: (D) -> String? = { null }): String
    fun D.signatureString(localNameResolver: (D) -> String? = { null }): String
    fun D.fqnString(localNameResolver: (D) -> String? = { null }): String

    fun D.hashedMangle(localNameResolver: (D) -> String? = { null }): Long = mangleString(localNameResolver).hashMangle
    fun D.signatureMangle(localNameResolver: (D) -> String? = { null }): Long {
        val sss = signatureString(localNameResolver)
        return sss.hashMangle
    }
    fun D.fqnMangle(localNameResolver: (D) -> String? = { null }): Long = fqnString(localNameResolver).hashMangle

    fun D.isPlatformSpecificExport(): Boolean = false

    val manglerName: String

    interface DescriptorMangler : KotlinMangler<DeclarationDescriptor> {
        override val manglerName: String
            get() = "Descriptor"

//        fun ClassDescriptor.isExportEnumEntry(): Boolean
        fun ClassDescriptor.mangleEnumEntryString(): String

//        fun PropertyDescriptor.isExportField(): Boolean
        fun PropertyDescriptor.mangleFieldString(): String

        fun setupTypeApproximation(app: (KotlinType) -> (KotlinType))
    }

    interface IrMangler : KotlinMangler<IrDeclaration> {
        override val manglerName: String
            get() = "Ir"
    }
}
