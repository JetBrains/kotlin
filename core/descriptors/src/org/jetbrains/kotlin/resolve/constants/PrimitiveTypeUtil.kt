/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.constants

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType

internal fun KotlinType.minValue(): Long {
    if (UnsignedTypes.isUnsignedType(this)) return 0
    return when {
        KotlinBuiltIns.isByte(this) -> Byte.MIN_VALUE.toLong()
        KotlinBuiltIns.isShort(this) -> Short.MIN_VALUE.toLong()
        KotlinBuiltIns.isInt(this) -> Int.MIN_VALUE.toLong()
        else -> error("Can't get min value for type: $this")
    }
}

internal fun KotlinType.maxValue(): Long {
    return when {
        KotlinBuiltIns.isByte(this) -> Byte.MAX_VALUE.toLong()
        KotlinBuiltIns.isShort(this) -> Short.MAX_VALUE.toLong()
        KotlinBuiltIns.isInt(this) -> Int.MAX_VALUE.toLong()

        KotlinBuiltIns.isUByte(this) -> UByte.MAX_VALUE.toLong()
        KotlinBuiltIns.isUShort(this) -> UShort.MAX_VALUE.toLong()
        KotlinBuiltIns.isUInt(this) -> UInt.MAX_VALUE.toLong()

        else -> error("Can't get max value for type: $this")
    }
}

internal fun ModuleDescriptor.unsignedType(classId: ClassId): SimpleType = findClassAcrossModuleDependencies(classId)!!.defaultType

internal val ModuleDescriptor.uIntType: SimpleType
    get() = unsignedType(StandardNames.FqNames.uInt)

internal val ModuleDescriptor.uLongType: SimpleType
    get() = unsignedType(StandardNames.FqNames.uLong)

internal val ModuleDescriptor.uByteType: SimpleType
    get() = unsignedType(StandardNames.FqNames.uByte)

internal val ModuleDescriptor.uShortType: SimpleType
    get() = unsignedType(StandardNames.FqNames.uShort)

internal val ModuleDescriptor.allSignedLiteralTypes: Collection<KotlinType>
    get() = listOf(builtIns.intType, builtIns.longType, builtIns.byteType, builtIns.shortType)

internal val ModuleDescriptor.allUnsignedLiteralTypes: Collection<KotlinType>
    get() = if (hasUnsignedTypesInModuleDependencies(this)) {
        listOf(
            unsignedType(StandardNames.FqNames.uInt), unsignedType(StandardNames.FqNames.uLong),
            unsignedType(StandardNames.FqNames.uByte), unsignedType(StandardNames.FqNames.uShort)
        )
    } else {
        emptyList()
    }
