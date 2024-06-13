/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.name.ClassId

public interface KaTypeInformationProvider {
    /**
     * Returns true if this type is denotable. A denotable type is a type that can be written in Kotlin by end users. See
     * https://kotlinlang.org/spec/type-system.html#type-kinds for more details.
     */
    public val KaType.isDenotable: Boolean

    /**
     * Returns true if this type is a functional interface type, a.k.a. SAM type, e.g., Runnable.
     */
    public val KaType.isFunctionalInterface: Boolean

    @Deprecated("Use 'isFunctionalInterface' instead.", replaceWith = ReplaceWith("isFunctionalInterface"))
    public val KaType.isFunctionalInterfaceType: Boolean
        get() = isFunctionalInterface

    /**
     * Returns [FunctionTypeKind] of the given [KaType]
     */
    @KaExperimentalApi
    public val KaType.functionTypeKind: FunctionTypeKind?

    @OptIn(KaExperimentalApi::class)
    public val KaType.isFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.Function }

    @OptIn(KaExperimentalApi::class)
    public val KaType.isKFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.KFunction }

    @OptIn(KaExperimentalApi::class)
    public val KaType.isSuspendFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.SuspendFunction }

    @OptIn(KaExperimentalApi::class)
    public val KaType.isKSuspendFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.KSuspendFunction }

    /**
     * Returns true if a public value of this type can potentially be null. This means this type is not a subtype of [Any]. However, it does not
     * mean one can assign `null` to a variable of this type because it may be unknown if this type can accept `null`. For example, a public value
     * of type `T:Any?` can potentially be null. But one can not assign `null` to such a variable because the instantiated type may not be
     * nullable.
     */
    public val KaType.canBeNull: Boolean

    /** Returns true if the type is explicitly marked as nullable. This means it's safe to assign `null` to a variable with this type. */
    public val KaType.isMarkedNullable: Boolean
        get() = withValidityAssertion { this.nullability == KaTypeNullability.NULLABLE }

    /** Returns true if the type is a platform flexible type and may or may not be marked nullable. */
    public val KaType.hasFlexibleNullability: Boolean
        get() = withValidityAssertion { this is KaFlexibleType && this.upperBound.isMarkedNullable != this.lowerBound.isMarkedNullable }

    public val KaType.isUnitType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.UNIT) }
    public val KaType.isIntType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.INT) }
    public val KaType.isLongType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.LONG) }
    public val KaType.isShortType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.SHORT) }
    public val KaType.isByteType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.BYTE) }
    public val KaType.isFloatType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.FLOAT) }
    public val KaType.isDoubleType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.DOUBLE) }
    public val KaType.isCharType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.CHAR) }
    public val KaType.isBooleanType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.BOOLEAN) }
    public val KaType.isStringType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.STRING) }
    public val KaType.isCharSequenceType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.CHAR_SEQUENCE) }
    public val KaType.isAnyType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.ANY) }
    public val KaType.isNothingType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.NOTHING) }

    public val KaType.isUIntType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uInt) }
    public val KaType.isULongType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uLong) }
    public val KaType.isUShortType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uShort) }
    public val KaType.isUByteType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uByte) }

    @Deprecated("Use 'isUnitType' instead.", replaceWith = ReplaceWith("isUnitType"))
    public val KaType.isUnit: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.UNIT) }

    @Deprecated("Use 'isIntType' instead.", replaceWith = ReplaceWith("isIntType"))
    public val KaType.isInt: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.INT) }

    @Deprecated("Use 'isLongType' instead.", replaceWith = ReplaceWith("isLongType"))
    public val KaType.isLong: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.LONG) }

    @Deprecated("Use 'isShortType' instead.", replaceWith = ReplaceWith("isShortType"))
    public val KaType.isShort: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.SHORT) }

    @Deprecated("Use 'isByteType' instead.", replaceWith = ReplaceWith("isByteType"))
    public val KaType.isByte: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.BYTE) }

    @Deprecated("Use 'isFloatType' instead.", replaceWith = ReplaceWith("isFloatType"))
    public val KaType.isFloat: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.FLOAT) }

    @Deprecated("Use 'isDoubleType' instead.", replaceWith = ReplaceWith("isDoubleType"))
    public val KaType.isDouble: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.DOUBLE) }

    @Deprecated("Use 'isCharType' instead.", replaceWith = ReplaceWith("isCharType"))
    public val KaType.isChar: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.CHAR) }

    @Deprecated("Use 'isBooleanType' instead.", replaceWith = ReplaceWith("isBooleanType"))
    public val KaType.isBoolean: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.BOOLEAN) }

    @Deprecated("Use 'isStringType' instead.", replaceWith = ReplaceWith("isStringType"))
    public val KaType.isString: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.STRING) }

    @Deprecated("Use 'isCharSequenceType' instead.", replaceWith = ReplaceWith("isCharSequenceType"))
    public val KaType.isCharSequence: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.CHAR_SEQUENCE) }

    @Deprecated("Use 'isAnyType' instead.", replaceWith = ReplaceWith("isAnyType"))
    public val KaType.isAny: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.ANY) }

    @Deprecated("Use 'isNothingType' instead.", replaceWith = ReplaceWith("isNothingType"))
    public val KaType.isNothing: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.NOTHING) }

    @Deprecated("Use 'isUIntType' instead.", replaceWith = ReplaceWith("isUIntType"))
    public val KaType.isUInt: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uInt) }

    @Deprecated("Use 'isULongType' instead.", replaceWith = ReplaceWith("isULongType"))
    public val KaType.isULong: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uLong) }

    @Deprecated("Use 'isUShortType' instead.", replaceWith = ReplaceWith("isUShortType"))
    public val KaType.isUShort: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uShort) }

    @Deprecated("Use 'isUByteType' instead.", replaceWith = ReplaceWith("isUByteType"))
    public val KaType.isUByte: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uByte) }

    /** Gets the class symbol backing the given type, if available. */
    public val KaType.expandedSymbol: KaClassOrObjectSymbol?
        get() = withValidityAssertion {
            return when (this) {
                is KaClassType -> when (val symbol = symbol) {
                    is KaClassOrObjectSymbol -> symbol
                    is KaTypeAliasSymbol -> symbol.expandedType.expandedSymbol
                }
                else -> null
            }
        }

    /** Gets the class symbol backing the given type, if available. */
    @Deprecated("Use 'expandedSymbol' instead.", ReplaceWith("expandedSymbol"))
    public val KaType.expandedClassSymbol: KaClassOrObjectSymbol?
        get() = expandedSymbol


    /**
     * Unwraps type aliases.
     * Example:
     * ```
     * interface Base
     *
     * typealias FirstAlias = @Anno1 Base
     * typealias SecondAlias = @Anno2 FirstAlias
     *
     * fun foo(): @Anno3 SecondAlias = TODO()
     * ```
     * The return type of `foo` will be `@Anno3 @Anno2 @Anno1 Base` instead of `@Anno3 SecondAlias`
     */
    public val KaType.fullyExpandedType: KaType

    /**
     * Returns whether the given [KaType] is an array or a primitive array type or not.
     */
    public val KaType.isArrayOrPrimitiveArray: Boolean

    /**
     * Returns whether the given [KaType] is an array or a primitive array type and its element is also an array type or not.
     */
    public val KaType.isNestedArray: Boolean

    public fun KaType.isClassType(classId: ClassId): Boolean = withValidityAssertion {
        if (this !is KaClassType) return false
        return this.classId == classId
    }

    @Deprecated("Use 'isClassType()' instead.", replaceWith = ReplaceWith("isClassType(classId)"))
    public fun KaType.isClassTypeWithClassId(classId: ClassId): Boolean = isClassType(classId)

    public val KaType.isPrimitive: Boolean
        get() = withValidityAssertion {
            if (this !is KaClassType) return false
            return this.classId in DefaultTypeClassIds.PRIMITIVES
        }

    @KaExperimentalApi
    public val KaType.defaultInitializer: String?
        get() = withValidityAssertion {
            when {
                isMarkedNullable -> "null"
                isIntType || isLongType || isShortType || isByteType -> "0"
                isFloatType -> "0.0f"
                isDoubleType -> "0.0"
                isCharType -> "'\\u0000'"
                isBooleanType -> "false"
                isUnitType -> "Unit"
                isStringType -> "\"\""
                isUIntType -> "0.toUInt()"
                isULongType -> "0.toULong()"
                isUShortType -> "0.toUShort()"
                isUByteType -> "0.toUByte()"
                else -> null
            }
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
