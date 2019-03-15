/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils


enum class UnsignedType(val classId: ClassId) {
    UBYTE(ClassId.fromString("kotlin/UByte")),
    USHORT(ClassId.fromString("kotlin/UShort")),
    UINT(ClassId.fromString("kotlin/UInt")),
    ULONG(ClassId.fromString("kotlin/ULong"));

    val typeName = classId.shortClassName
    val arrayClassId = ClassId(classId.packageFqName, Name.identifier(typeName.asString() + "Array"))
}

object UnsignedTypes {
    private val unsignedTypeNames = enumValues<UnsignedType>().map { it.typeName }.toSet()
    private val arrayClassIdToUnsignedClassId = hashMapOf<ClassId, ClassId>()
    private val unsignedClassIdToArrayClassId = hashMapOf<ClassId, ClassId>()

    private val arrayClassesShortNames: Set<Name> = UnsignedType.values().mapTo(mutableSetOf()) { it.arrayClassId.shortClassName }

    init {
        for (unsignedType in UnsignedType.values()) {
            arrayClassIdToUnsignedClassId[unsignedType.arrayClassId] = unsignedType.classId
            unsignedClassIdToArrayClassId[unsignedType.classId] = unsignedType.arrayClassId
        }
    }

    fun isShortNameOfUnsignedArray(name: Name) = name in arrayClassesShortNames
    fun isShortNameOfUnsignedType(name: Name) = name in unsignedTypeNames

    fun getUnsignedClassIdByArrayClassId(arrayClassId: ClassId): ClassId? = arrayClassIdToUnsignedClassId[arrayClassId]
    fun getUnsignedArrayClassIdByUnsignedClassId(arrayClassId: ClassId): ClassId? = unsignedClassIdToArrayClassId[arrayClassId]

    fun isUnsignedType(type: KotlinType): Boolean {
        if (TypeUtils.noExpectedType(type)) return false

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
