/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

/**
 * Most "underlying" user-visible non-reference type.
 * It is visible as inlined to compiler for simplicity.
 */
enum class KonanPrimitiveType(val classId: ClassId, val binaryType: BinaryType.Primitive) {
    BOOLEAN(PrimitiveType.BOOLEAN, PrimitiveBinaryType.BOOLEAN),
    CHAR(PrimitiveType.CHAR, PrimitiveBinaryType.SHORT),
    BYTE(PrimitiveType.BYTE, PrimitiveBinaryType.BYTE),
    SHORT(PrimitiveType.SHORT, PrimitiveBinaryType.SHORT),
    INT(PrimitiveType.INT, PrimitiveBinaryType.INT),
    LONG(PrimitiveType.LONG, PrimitiveBinaryType.LONG),
    FLOAT(PrimitiveType.FLOAT, PrimitiveBinaryType.FLOAT),
    DOUBLE(PrimitiveType.DOUBLE, PrimitiveBinaryType.DOUBLE),
    NON_NULL_NATIVE_PTR(ClassId.Companion.topLevel(KonanFqNames.nonNullNativePtr.toSafe()), PrimitiveBinaryType.POINTER),
    VECTOR128(ClassId.Companion.topLevel(KonanFqNames.Vector128), PrimitiveBinaryType.VECTOR128)

    ;

    constructor(primitiveType: PrimitiveType, primitiveBinaryType: PrimitiveBinaryType)
            : this(ClassId.Companion.topLevel(primitiveType.typeFqName), primitiveBinaryType)

    constructor(classId: ClassId, primitiveBinaryType: PrimitiveBinaryType)
            : this(classId, BinaryType.Primitive(primitiveBinaryType))

    val fqName: FqNameUnsafe get() = this.classId.asSingleFqName().toUnsafe()

    companion object {
        val byFqNameParts = entries.toTypedArray().groupingBy {
            assert(!it.classId.isNestedClass)
            it.classId.packageFqName
        }.fold({ _, _ -> mutableMapOf<Name, KonanPrimitiveType>() },
               { _, accumulator, element ->
                   accumulator.also { it[element.classId.shortClassName] = element }
               })
    }
}