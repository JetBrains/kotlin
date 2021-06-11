/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeNullability
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeWithNullability
import org.jetbrains.kotlin.name.ClassId

abstract class KtTypeInfoProvider : KtAnalysisSessionComponent() {
    abstract fun canBeNull(type: KtType): Boolean
}

interface KtTypeInfoProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Returns true if a value of this type can potentially be null. This means this type is not a subtype of [Any]. However, it does not
     * mean one can assign `null` to a variable of this type because it may be unknown if this type can accept `null`. For example, a value
     * of type `T:Any?` can potentially be null. But one can not assign `null` to such a variable because the instantiated type may not be
     * nullable.
     */
    val KtType.canBeNull: Boolean get() = analysisSession.typeInfoProvider.canBeNull(this)

    /** Returns true if the type is explicitly marked as nullable. This means it's safe to assign `null` to a variable with this type. */
    val KtType.isMarkedNullable: Boolean get() = (this as? KtTypeWithNullability)?.nullability == KtTypeNullability.NULLABLE

    val KtType.isUnit: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.UNIT)
    val KtType.isInt: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.INT)
    val KtType.isLong: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.LONG)
    val KtType.isShort: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.SHORT)
    val KtType.isByte: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.BYTE)
    val KtType.isFloat: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.FLOAT)
    val KtType.isDouble: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.DOUBLE)
    val KtType.isChar: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.CHAR)
    val KtType.isBoolean: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.BOOLEAN)
    val KtType.isString: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.STRING)
    val KtType.isCharSequence: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.CHAR_SEQUENCE)
    val KtType.isAny: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.ANY)

    val KtType.isUInt: Boolean get() = isClassTypeWithClassId(StandardNames.FqNames.uInt)
    val KtType.isULong: Boolean get() = isClassTypeWithClassId(StandardNames.FqNames.uLong)
    val KtType.isUShort: Boolean get() = isClassTypeWithClassId(StandardNames.FqNames.uShort)
    val KtType.isUByte: Boolean get() = isClassTypeWithClassId(StandardNames.FqNames.uByte)

    fun KtType.isClassTypeWithClassId(classId: ClassId): Boolean {
        if (this !is KtClassType) return false
        return this.classId == classId
    }

    val KtType.isPrimitive: Boolean
        get() {
            if (this !is KtClassType) return false
            return this.classId in DefaultTypeClassIds.PRIMITIVES
        }

    val KtType.defaultInitializer: String?
        get() = when {
            isMarkedNullable -> "null"
            isInt || isLong || isShort || isByte -> "0"
            isFloat -> "0.0f"
            isDouble -> "0.0"
            isChar -> "'\\u0000'"
            isBoolean -> "false"
            isUnit -> "Unit"
            isString -> "\"\""
            isUInt -> "0.toUInt()"
            isULong -> "0.toULong()"
            isUShort -> "0.toUShort()"
            isUByte -> "0.toUByte()"
            else -> null
        }
}

object DefaultTypeClassIds {
    val UNIT = ClassId.topLevel(StandardNames.FqNames.unit.toSafe())
    val INT = ClassId.topLevel(StandardNames.FqNames._int.toSafe())
    val LONG = ClassId.topLevel(StandardNames.FqNames._long.toSafe())
    val SHORT = ClassId.topLevel(StandardNames.FqNames._short.toSafe())
    val BYTE = ClassId.topLevel(StandardNames.FqNames._byte.toSafe())
    val FLOAT = ClassId.topLevel(StandardNames.FqNames._float.toSafe())
    val DOUBLE = ClassId.topLevel(StandardNames.FqNames._double.toSafe())
    val CHAR = ClassId.topLevel(StandardNames.FqNames._char.toSafe())
    val BOOLEAN = ClassId.topLevel(StandardNames.FqNames._boolean.toSafe())
    val STRING = ClassId.topLevel(StandardNames.FqNames.string.toSafe())
    val CHAR_SEQUENCE = ClassId.topLevel(StandardNames.FqNames.charSequence.toSafe())
    val ANY = ClassId.topLevel(StandardNames.FqNames.any.toSafe())
    val PRIMITIVES = setOf(INT, LONG, SHORT, BYTE, FLOAT, DOUBLE, CHAR, BOOLEAN)
}
