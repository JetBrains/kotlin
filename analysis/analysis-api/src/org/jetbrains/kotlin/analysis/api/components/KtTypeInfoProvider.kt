/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.name.ClassId

public abstract class KaTypeInfoProvider : KaSessionComponent() {
    public abstract fun isFunctionalInterfaceType(type: KaType): Boolean
    public abstract fun getFunctionClassKind(type: KaType): FunctionTypeKind?
    public abstract fun canBeNull(type: KaType): Boolean
    public abstract fun isDenotable(type: KaType): Boolean
    public abstract fun isArrayOrPrimitiveArray(type: KaType): Boolean
    public abstract fun isNestedArray(type: KaType): Boolean
    public abstract fun fullyExpandedType(type: KaType): KaType
}

public typealias KtTypeInfoProvider = KaTypeInfoProvider

public interface KaTypeInfoProviderMixIn : KaSessionMixIn {
    /**
     * Returns true if this type is denotable. A denotable type is a type that can be written in Kotlin by end users. See
     * https://kotlinlang.org/spec/type-system.html#type-kinds for more details.
     */
    public val KaType.isDenotable: Boolean
        get() = withValidityAssertion { analysisSession.typeInfoProvider.isDenotable(this) }

    /**
     * Returns true if this type is a functional interface type, a.k.a. SAM type, e.g., Runnable.
     */
    public val KaType.isFunctionalInterfaceType: Boolean
        get() = withValidityAssertion { analysisSession.typeInfoProvider.isFunctionalInterfaceType(this) }

    /**
     * Returns [FunctionTypeKind] of the given [KaType]
     */
    public val KaType.functionTypeKind: FunctionTypeKind?
        get() = withValidityAssertion { analysisSession.typeInfoProvider.getFunctionClassKind(this) }

    public val KaType.isFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.Function }

    public val KaType.isKFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.KFunction }

    public val KaType.isSuspendFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.SuspendFunction }

    public val KaType.isKSuspendFunctionType: Boolean
        get() = withValidityAssertion { functionTypeKind == FunctionTypeKind.KSuspendFunction }

    /**
     * Returns true if a public value of this type can potentially be null. This means this type is not a subtype of [Any]. However, it does not
     * mean one can assign `null` to a variable of this type because it may be unknown if this type can accept `null`. For example, a public value
     * of type `T:Any?` can potentially be null. But one can not assign `null` to such a variable because the instantiated type may not be
     * nullable.
     */
    public val KaType.canBeNull: Boolean get() = withValidityAssertion { analysisSession.typeInfoProvider.canBeNull(this) }

    /** Returns true if the type is explicitly marked as nullable. This means it's safe to assign `null` to a variable with this type. */
    public val KaType.isMarkedNullable: Boolean get() = withValidityAssertion { this.nullability == KaTypeNullability.NULLABLE }

    /** Returns true if the type is a platform flexible type and may or may not be marked nullable. */
    public val KaType.hasFlexibleNullability: Boolean get() = withValidityAssertion { this is KaFlexibleType && this.upperBound.isMarkedNullable != this.lowerBound.isMarkedNullable }

    public val KaType.isUnit: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.UNIT) }
    public val KaType.isInt: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.INT) }
    public val KaType.isLong: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.LONG) }
    public val KaType.isShort: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.SHORT) }
    public val KaType.isByte: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.BYTE) }
    public val KaType.isFloat: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.FLOAT) }
    public val KaType.isDouble: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.DOUBLE) }
    public val KaType.isChar: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.CHAR) }
    public val KaType.isBoolean: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.BOOLEAN) }
    public val KaType.isString: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.STRING) }
    public val KaType.isCharSequence: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.CHAR_SEQUENCE) }
    public val KaType.isAny: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.ANY) }
    public val KaType.isNothing: Boolean get() = withValidityAssertion { isClassTypeWithClassId(DefaultTypeClassIds.NOTHING) }

    public val KaType.isUInt: Boolean get() = withValidityAssertion { isClassTypeWithClassId(StandardNames.FqNames.uInt) }
    public val KaType.isULong: Boolean get() = withValidityAssertion { isClassTypeWithClassId(StandardNames.FqNames.uLong) }
    public val KaType.isUShort: Boolean get() = withValidityAssertion { isClassTypeWithClassId(StandardNames.FqNames.uShort) }
    public val KaType.isUByte: Boolean get() = withValidityAssertion { isClassTypeWithClassId(StandardNames.FqNames.uByte) }

    /** Gets the class symbol backing the given type, if available. */
    public val KaType.expandedSymbol: KaClassOrObjectSymbol?
        get() = withValidityAssertion {
            return when (this) {
                is KaNonErrorClassType -> when (val symbol = symbol) {
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
        get() = withValidityAssertion {
            analysisSession.typeInfoProvider.fullyExpandedType(this)
        }

    /**
     * Returns whether the given [KaType] is an array or a primitive array type or not.
     */
    public fun KaType.isArrayOrPrimitiveArray(): Boolean =
        withValidityAssertion { analysisSession.typeInfoProvider.isArrayOrPrimitiveArray(this) }

    /**
     * Returns whether the given [KaType] is an array or a primitive array type and its element is also an array type or not.
     */
    public fun KaType.isNestedArray(): Boolean = withValidityAssertion { analysisSession.typeInfoProvider.isNestedArray(this) }

    public fun KaType.isClassTypeWithClassId(classId: ClassId): Boolean = withValidityAssertion {
        if (this !is KaNonErrorClassType) return false
        return this.classId == classId
    }

    public val KaType.isPrimitive: Boolean
        get() = withValidityAssertion {
            if (this !is KaNonErrorClassType) return false
            return this.classId in DefaultTypeClassIds.PRIMITIVES
        }

    public val KaType.defaultInitializer: String?
        get() = withValidityAssertion {
            when {
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
}

public typealias KtTypeInfoProviderMixIn = KaTypeInfoProviderMixIn

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
