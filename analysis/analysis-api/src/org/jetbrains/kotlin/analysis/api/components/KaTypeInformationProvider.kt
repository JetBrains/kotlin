/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.name.ClassId

public interface KaTypeInformationProvider {
    /**
     * `true` if this type is denotable.
     * A denotable type is a type that can be written in Kotlin by a developer.
     *
     * See the [Kotlin language specification](https://kotlinlang.org/spec/type-system.html#type-kinds) for more details.
     */
    public val KaType.isDenotable: Boolean

    /**
     * `true` if this type is a functional interface type, a.k.a. SAM type, e.g., [Runnable].
     */
    public val KaType.isFunctionalInterface: Boolean

    /**
     * A [FunctionTypeKind] of the given [KaType], or `null` if the type is not a function type.
     */
    @KaExperimentalApi
    public val KaType.functionTypeKind: FunctionTypeKind?

    /**
     * `true` if this type is a [kotlin.Function] type.
     */
    @OptIn(KaExperimentalApi::class)
    public val KaType.isFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.Function }

    /**
     * `true` if this type is a [kotlin.reflect.KFunction] type.
     */
    @OptIn(KaExperimentalApi::class)
    public val KaType.isKFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.KFunction }

    /**
     * `true` if this type is a [kotlin.coroutines.SuspendFunction] type.
     */
    @OptIn(KaExperimentalApi::class)
    public val KaType.isSuspendFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.SuspendFunction }

    /**
     * `true` if this type is a [kotlin.reflect.KSuspendFunction] type.
     */
    @OptIn(KaExperimentalApi::class)
    public val KaType.isKSuspendFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.KSuspendFunction }

    /**
     * `true` if a public value of this type can potentially be `null`.
     *
     * This means this type is not a subtype of [Any].
     * However, it does not mean one can assign `null` to a variable of this type because it may be unknown if this type can accept `null`.
     *
     * For example, a public value of type `T:Any?` can potentially be null.
     * But one cannot assign `null` to such a variable because the instantiated type may not be nullable.
     */
    public val KaType.canBeNull: Boolean

    /**
     * `true` if the type is explicitly marked as nullable.
     * This means it is safe to assign `null` to a variable with this type.
     * */
    public val KaType.isMarkedNullable: Boolean
        get() = withValidityAssertion { this.nullability == KaTypeNullability.NULLABLE }

    /** `true` if the type is a flexible (platform) type, can both safe and ordinary calls are valid on it. */
    public val KaType.hasFlexibleNullability: Boolean
        get() = withValidityAssertion { this is KaFlexibleType && this.upperBound.isMarkedNullable != this.lowerBound.isMarkedNullable }

    /** `true` if the type is a [Unit] type. */
    public val KaType.isUnitType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.UNIT) }

    /** `true` if the type is an [Int] type. */
    public val KaType.isIntType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.INT) }

    /** `true` if the type is a [Long] type. */
    public val KaType.isLongType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.LONG) }

    /** `true` if the type is a [Short] type. */
    public val KaType.isShortType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.SHORT) }

    /** `true` if the type is a [Byte] type. */
    public val KaType.isByteType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.BYTE) }

    /** `true` if the type is a [Float] type. */
    public val KaType.isFloatType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.FLOAT) }

    /** `true` if the type is a [Double] type. */
    public val KaType.isDoubleType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.DOUBLE) }

    /** `true` if the type is a [Char] type. */
    public val KaType.isCharType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.CHAR) }

    /** `true` if the type is a [Boolean] type. */
    public val KaType.isBooleanType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.BOOLEAN) }

    /** `true` if the type is a [String] type. */
    public val KaType.isStringType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.STRING) }

    /** `true` if the type is a [CharSequence] type. */
    public val KaType.isCharSequenceType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.CHAR_SEQUENCE) }

    /** `true` if the type is an [Any] type. */
    public val KaType.isAnyType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.ANY) }

    /** `true` if the type is a [Nothing] type. */
    public val KaType.isNothingType: Boolean get() = withValidityAssertion { isClassType(DefaultTypeClassIds.NOTHING) }

    /** `true` if the type is a [UInt] type. */
    public val KaType.isUIntType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uInt) }

    /** `true` if the type is a [ULong] type. */
    public val KaType.isULongType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uLong) }

    /** `true` if the type is a [UShort] type. */
    public val KaType.isUShortType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uShort) }

    /** `true` if the type is a [UByte] type. */
    public val KaType.isUByteType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uByte) }

    /** The class symbol backing the given type, if available. */
    public val KaType.expandedSymbol: KaClassSymbol?
        get() = withValidityAssertion {
            return when (this) {
                is KaClassType -> when (val symbol = symbol) {
                    is KaClassSymbol -> symbol
                    is KaTypeAliasSymbol -> symbol.expandedType.expandedSymbol
                }
                else -> null
            }
        }

    /**
     * Unwraps type aliases.
     *
     * Example:
     * ```kotlin
     * interface Base
     *
     * typealias FirstAlias = @Anno1 Base
     * typealias SecondAlias = @Anno2 FirstAlias
     *
     * fun foo(): @Anno3 SecondAlias = TODO()
     * ```
     *
     * The return type of `foo()` will be `@Anno3 @Anno2 @Anno1 Base` instead of `@Anno3 SecondAlias`
     */
    public val KaType.fullyExpandedType: KaType

    /**
     * `true` if the given [KaType] is an array or a primitive array type.
     */
    public val KaType.isArrayOrPrimitiveArray: Boolean

    /**
     * `true` if the given [KaType] is an array or a primitive array type, and if its element is also an array type.
     */
    public val KaType.isNestedArray: Boolean

    /**
     * Checks whether the given [KaType] is a class type with the given [ClassId].
     */
    public fun KaType.isClassType(classId: ClassId): Boolean = withValidityAssertion {
        if (this !is KaClassType) return false
        return this.classId == classId
    }

    /**
     * `true` if the given [KaType] is a primitive type.
     */
    public val KaType.isPrimitive: Boolean
        get() = withValidityAssertion {
            if (this !is KaClassType) return false
            return this.classId in DefaultTypeClassIds.PRIMITIVES
        }

    /**
     * A stub default initializer for the given type, or `null` if the type is neither a primitive nor a string.
     */
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
    /** The [Unit] class id. */
    public val UNIT: ClassId = ClassId.topLevel(StandardNames.FqNames.unit.toSafe())

    /** The [Int] class id. */
    public val INT: ClassId = ClassId.topLevel(StandardNames.FqNames._int.toSafe())

    /** The [Long] class id. */
    public val LONG: ClassId = ClassId.topLevel(StandardNames.FqNames._long.toSafe())

    /** The [Short] class id. */
    public val SHORT: ClassId = ClassId.topLevel(StandardNames.FqNames._short.toSafe())

    /** The [Byte] class id. */
    public val BYTE: ClassId = ClassId.topLevel(StandardNames.FqNames._byte.toSafe())

    /** The [Float] class id. */
    public val FLOAT: ClassId = ClassId.topLevel(StandardNames.FqNames._float.toSafe())

    /** The [Double] class id. */
    public val DOUBLE: ClassId = ClassId.topLevel(StandardNames.FqNames._double.toSafe())

    /** The [Char] class id. */
    public val CHAR: ClassId = ClassId.topLevel(StandardNames.FqNames._char.toSafe())

    /** The [Boolean] class id. */
    public val BOOLEAN: ClassId = ClassId.topLevel(StandardNames.FqNames._boolean.toSafe())

    /** The [String] class id. */
    public val STRING: ClassId = ClassId.topLevel(StandardNames.FqNames.string.toSafe())

    /** The [CharSequence] class id. */
    public val CHAR_SEQUENCE: ClassId = ClassId.topLevel(StandardNames.FqNames.charSequence.toSafe())

    /** The [Any] class id. */
    public val ANY: ClassId = ClassId.topLevel(StandardNames.FqNames.any.toSafe())

    /** The [Nothing] class id. */
    public val NOTHING: ClassId = ClassId.topLevel(StandardNames.FqNames.nothing.toSafe())

    /** A set of primitive class ids. */
    public val PRIMITIVES: Set<ClassId> = setOf(INT, LONG, SHORT, BYTE, FLOAT, DOUBLE, CHAR, BOOLEAN)
}
