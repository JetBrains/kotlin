/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import kotlin.reflect.KClass


enum class UnsignedType(val typeName: Name) {
    UBYTE("UByte"), USHORT("UShort"), UINT("UInt"), ULONG("ULong");

    constructor(typeName: String) : this(Name.identifier(typeName))
}

object UnsignedTypes {
    val unsignedTypeNames = enumValues<UnsignedType>().map { it.typeName }.toSet()

    fun isUnsignedType(type: KotlinType): Boolean {
        val descriptor = type.constructor.declarationDescriptor ?: return false
        return isUnsignedClass(descriptor)
    }

    fun isUnsignedClass(descriptor: DeclarationDescriptor): Boolean {
        val container = descriptor.containingDeclaration
        return container is PackageFragmentDescriptor &&
                container.fqName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME &&
                descriptor.name in UnsignedTypes.unsignedTypeNames
    }
}