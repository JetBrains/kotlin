/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.name.ClassId

public abstract class KtTypeInfoProvider : KtAnalysisSessionComponent() {
    public abstract fun isFunctionalInterfaceType(type: KtType): Boolean
    public abstract fun canBeNull(type: KtType): Boolean
}

public interface KtTypeInfoProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Returns true if this type is a functional interface type, a.k.a. SAM type, e.g., Runnable.
     */
    public val KtType.isFunctionalInterfaceType: Boolean
        get() = analysisSession.typeInfoProvider.isFunctionalInterfaceType(this)

    /**
     * Returns true if a public value of this type can potentially be null. This means this type is not a subtype of [Any]. However, it does not
     * mean one can assign `null` to a variable of this type because it may be unknown if this type can accept `null`. For example, a public value
     * of type `T:Any?` can potentially be null. But one can not assign `null` to such a variable because the instantiated type may not be
     * nullable.
     */
    public val KtType.canBeNull: Boolean get() = analysisSession.typeInfoProvider.canBeNull(this)

    /** Returns true if the type is explicitly marked as nullable. This means it's safe to assign `null` to a variable with this type. */
    public val KtType.isMarkedNullable: Boolean get() = this.nullability == KtTypeNullability.NULLABLE

    public val KtType.isUnit: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.UNIT)
    public val KtType.isInt: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.INT)
    public val KtType.isLong: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.LONG)
    public val KtType.isShort: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.SHORT)
    public val KtType.isByte: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.BYTE)
    public val KtType.isFloat: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.FLOAT)
    public val KtType.isDouble: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.DOUBLE)
    public val KtType.isChar: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.CHAR)
    public val KtType.isBoolean: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.BOOLEAN)
    public val KtType.isString: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.STRING)
    public val KtType.isCharSequence: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.CHAR_SEQUENCE)
    public val KtType.isAny: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.ANY)
    public val KtType.isNothing: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.NOTHING)

    public val KtType.isUInt: Boolean get() = isClassTypeWithClassId(StandardNames.FqNames.uInt)
    public val KtType.isULong: Boolean get() = isClassTypeWithClassId(StandardNames.FqNames.uLong)
    public val KtType.isUShort: Boolean get() = isClassTypeWithClassId(StandardNames.FqNames.uShort)
    public val KtType.isUByte: Boolean get() = isClassTypeWithClassId(StandardNames.FqNames.uByte)

    /** Gets the class symbol backing the given type, if available. */
    public val KtType.expandedClassSymbol: KtClassOrObjectSymbol?
        get() {
            return when (this) {
                is KtNonErrorClassType -> when (val classSymbol = classSymbol) {
                    is KtClassOrObjectSymbol -> classSymbol
                    is KtTypeAliasSymbol -> classSymbol.expandedType.expandedClassSymbol
                }
                else -> null
            }
        }

    public fun KtType.isClassTypeWithClassId(classId: ClassId): Boolean {
        if (this !is KtNonErrorClassType) return false
        return this.classId == classId
    }

    public val KtType.isPrimitive: Boolean
        get() {
            if (this !is KtNonErrorClassType) return false
            return this.classId in DefaultTypeClassIds.PRIMITIVES
        }

    public val KtType.defaultInitializer: String?
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

public object DefaultTypeClassIds {
    public val UNIT: ClassId = ClassId.topLevel(StandardNames.FqNames.unit.toSafe())
    public val INT: ClassId = ClassId.topLevel(StandardNames.FqNames._int.toSafe())
    public val LONG: ClassId = ClassId.topLevel(StandardNames.FqNames._long.toSafe())
    public val SHORT: ClassId = ClassId.topLevel(StandardNames.FqNames._short.toSafe())
    public val BYTE: ClassId = ClassId.topLevel(StandardNames.FqNames._byte.toSafe())
    public val FLOAT: ClassId = ClassId.topLevel(StandardNames.FqNames._float.toSafe())
    public val DOUBLE: ClassId = ClassId.topLevel(StandardNames.FqNames._double.toSafe())
    public val CHAR: ClassId = ClassId.topLevel(StandardNames.FqNames._char.toSafe())
    public val BOOLEAN: ClassId = ClassId.topLevel(StandardNames.FqNames._boolean.toSafe())
    public val STRING: ClassId = ClassId.topLevel(StandardNames.FqNames.string.toSafe())
    public val CHAR_SEQUENCE: ClassId = ClassId.topLevel(StandardNames.FqNames.charSequence.toSafe())
    public val ANY: ClassId = ClassId.topLevel(StandardNames.FqNames.any.toSafe())
    public val NOTHING: ClassId = ClassId.topLevel(StandardNames.FqNames.nothing.toSafe())
    public val PRIMITIVES: Set<ClassId> = setOf(INT, LONG, SHORT, BYTE, FLOAT, DOUBLE, CHAR, BOOLEAN)
}
