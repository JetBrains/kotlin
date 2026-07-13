/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.name.ClassId

@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaTypeInformationProvider : KaSessionComponent {
    /**
     * Whether the [KaType] is denotable. A [denotable type](https://kotlinlang.org/spec/type-system.html#type-kinds) can be expressed in
     * Kotlin code, as opposed to being only constructible via compiler type operations (such as type inference).
     */
    public val KaType.isDenotable: Boolean

    /**
     * Whether the [KaType] is a [functional interface type](https://kotlinlang.org/docs/fun-interfaces.html), such as [Runnable]. Such
     * types are also known as SAM types.
     */
    public val KaType.isFunctionalInterface: Boolean

    /**
     * The [FunctionTypeKind] of the given [KaType], or `null` if the type is not a function type.
     */
    @KaExperimentalApi
    @Deprecated("Use 'functionTypeFamily' instead", level = DeprecationLevel.HIDDEN)
    @KaNoContextParameterBridgeRequired
    public val KaType.functionTypeKind: FunctionTypeKind?

    /**
     * The [function type family][KaFunctionTypeFamily] of the given [KaType], or `null` if the type is not a function type.
     *
     * For example, `(Int) -> String` belongs to the [Function][KaBuiltinFunctionTypeFamilies.function] family,
     * while `suspend () -> Unit` belongs to the [SuspendFunction][KaBuiltinFunctionTypeFamilies.suspendFunction] family.
     *
     * @see KaBuiltinFunctionTypeFamilies
     */
    @KaExperimentalApi
    public val KaType.functionTypeFamily: KaFunctionTypeFamily?

    /**
     * Whether the [KaType] is a [kotlin.Function] type.
     */
    @OptIn(KaExperimentalApi::class)
    public val KaType.isFunctionType: Boolean
        get() = withValidityAssertion { functionTypeFamily == builtinFunctionTypeFamilies.function }

    /**
     * Whether the [KaType] is a [kotlin.reflect.KFunction] type.
     */
    @OptIn(KaExperimentalApi::class)
    public val KaType.isKFunctionType: Boolean
        get() = withValidityAssertion { functionTypeFamily == builtinFunctionTypeFamilies.kFunction }

    /**
     * Whether the [KaType] is a [suspend function](https://kotlinlang.org/spec/asynchronous-programming-with-coroutines.html#suspending-functions)
     * type.
     */
    @OptIn(KaExperimentalApi::class)
    public val KaType.isSuspendFunctionType: Boolean
        get() = withValidityAssertion { functionTypeFamily == builtinFunctionTypeFamilies.suspendFunction }

    /**
     * Whether the [KaType] is a `KSuspendFunction` type.
     */
    @OptIn(KaExperimentalApi::class)
    public val KaType.isKSuspendFunctionType: Boolean
        get() = withValidityAssertion { functionTypeFamily == builtinFunctionTypeFamilies.kSuspendFunction }

    /**
     * Whether a public value of the [KaType] can potentially be `null`.
     *
     * If a type can be `null`, it means that this type is not a subtype of [Any]. However, it does not mean one can assign `null` to a
     * variable of this type. It may be unknown whether this type can accept `null`.
     *
     * #### Example
     *
     * A public value of type `T : Any?` can potentially be `null`. But one cannot assign `null` to such a variable because the instantiated
     * type may not be nullable.
     */
    @KaNoContextParameterBridgeRequired
    @Deprecated("Use `isNullable` instead", ReplaceWith("this.isNullable"))
    public val KaType.canBeNull: Boolean
        get() = isNullable

    /**
     * Whether a public value of the [KaType] can potentially be `null`.
     *
     * If a type can be `null`, it means that this type is not a subtype of [Any]. However, it does not mean one can assign `null` to a
     * variable of this type. It may be unknown whether this type can accept `null`.
     *
     * #### Example
     *
     * A public value of type `T : Any?` can potentially be `null`. But one cannot assign `null` to such a variable because the instantiated
     * type may not be nullable.
     */
    public val KaType.isNullable: Boolean

    /**
     * Whether the [KaType] is explicitly marked as nullable, i.e., is represented as `T?`.
     *
     * Note that this property just reflects the presence of nullability in the type signature,
     * and sometimes [isMarkedNullable] being false doesn't imply that the given type cannot hold `null` or be assigned with it.
     *
     * For example, [isMarkedNullable] doesn't expand type aliases to check the nullability of their underlying type:
     * ```kotlin
     * typealias NonMarkedNullableAlias = String?
     *
     * fun main() {
     *     val x: NonMarkedNullableAlias = null
     * }
     * ```
     * The type of `x` is `NonMarkedNullableAlias`, which is not marked as nullable. However, it still represents a nullable type and can hold `null` and can be assigned with that.
     *
     * To explicitly check whether a type can potentially hold `null`, use [isNullable].
     */
    public val KaType.isMarkedNullable: Boolean

    /**
     * Whether the [KaType] is a [org.jetbrains.kotlin.analysis.api.types.KaFlexibleType] / [org.jetbrains.kotlin.analysis.api.types.KaDynamicType] with flexible nullability or [org.jetbrains.kotlin.analysis.api.types.KaErrorType] with unknown nullability.
     * Both safe and ordinary calls are valid on such types.
     *
     * Note that a flexible / dynamic type has a flexible nullability when the lower bound is non-nullable and the upper bound is nullable.
     * E.g. `T!` has `T` as the lower bound and `T?` as the upper bound, hence it has a flexible nullability.
     */
    public val KaType.hasFlexibleNullability: Boolean

    /**
     * Whether the [KaType] is a [Unit] type.
     */
    public val KaType.isUnitType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.UNIT) }

    /**
     * Whether the [KaType] is an [Int] type.
     */
    public val KaType.isIntType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.INT) }

    /**
     * Whether the [KaType] is a [Long] type.
     */
    public val KaType.isLongType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.LONG) }

    /**
     * Whether the [KaType] is a [Short] type.
     */
    public val KaType.isShortType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.SHORT) }

    /**
     * Whether the [KaType] is a [Byte] type.
     */
    public val KaType.isByteType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.BYTE) }

    /**
     * Whether the [KaType] is a [Float] type.
     */
    public val KaType.isFloatType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.FLOAT) }

    /**
     * Whether the [KaType] is a [Double] type.
     */
    public val KaType.isDoubleType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.DOUBLE) }

    /**
     * Whether the [KaType] is a [Char] type.
     */
    public val KaType.isCharType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.CHAR) }

    /**
     * Whether the [KaType] is a [Boolean] type.
     */
    public val KaType.isBooleanType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.BOOLEAN) }

    /**
     * Whether the [KaType] is a [String] type.
     */
    public val KaType.isStringType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.STRING) }

    /**
     * Whether the [KaType] is a [CharSequence] type.
     */
    public val KaType.isCharSequenceType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.CHAR_SEQUENCE) }

    /**
     * Whether the [KaType] is an [Any] type.
     */
    public val KaType.isAnyType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.ANY) }

    /**
     * Whether the [KaType] is a [Nothing] type.
     */
    public val KaType.isNothingType: Boolean get() = withValidityAssertion { isClassType(org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.NOTHING) }

    /**
     * Whether the [KaType] is a [UInt] type.
     */
    public val KaType.isUIntType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uInt) }

    /**
     * Whether the [KaType] is a [ULong] type.
     */
    public val KaType.isULongType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uLong) }

    /**
     * Whether the [KaType] is a [UShort] type.
     */
    public val KaType.isUShortType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uShort) }

    /**
     * Whether the [KaType] is a [UByte] type.
     */
    public val KaType.isUByteType: Boolean get() = withValidityAssertion { isClassType(StandardNames.FqNames.uByte) }

    /**
     * The class symbol backing the given [KaType], if available.
     */
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
     * The type that corresponds to the given [KaType] with fully expanded type aliases.
     *
     * Type aliases are usually expanded immediately by the compiler, so most [KaType]s should already present in their expanded forms.
     * Nonetheless, it is possible to obtain unexpanded types from the Analysis API, and [fullyExpandedType] may be used to expand type
     * aliases in such types.
     *
     * #### Example
     *
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
     *
     * @see KaType.abbreviation
     */
    public val KaType.fullyExpandedType: KaType

    /**
     * Whether the [KaType] is an array or a primitive array type.
     */
    public val KaType.isArrayOrPrimitiveArray: Boolean

    /**
     * Whether the [KaType] is an array or a primitive array type, and its element is also an array type.
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
     * Whether the [KaType] is a primitive type.
     */
    public val KaType.isPrimitive: Boolean
        get() = withValidityAssertion {
            if (this !is KaClassType) return false
            return this.classId in org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.PRIMITIVES
        }

    /**
     * The default initializer for the given [KaType], or `null` if the type is neither nullable, a primitive, nor a string.
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

    /**
     * Provides access to the built-in [function type families][KaFunctionTypeFamily].
     */
    @KaExperimentalApi
    public val builtinFunctionTypeFamilies: KaBuiltinFunctionTypeFamilies
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.types.KaFunctionTypeFamily] instead.**
 *
 * Describes a family of numbered function types such as `Function0`, `Function1`, ..., `FunctionN`.
 *
 * Kotlin has the following built-in function type families:
 * - `Function` — regular function types, e.g., `(Int) -> String`
 * - `SuspendFunction` — suspend function types, e.g., `suspend () -> Unit`
 * - `KFunction` — reflection types for regular functions
 * - `KSuspendFunction` — reflection types for suspend functions
 *
 * Compiler plugins may introduce additional custom function type families.
 *
 * @see KaTypeInformationProvider.functionTypeFamily
 * @see KaBuiltinFunctionTypeFamilies
 */
@KaObsoleteComponentApi
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaFunctionTypeFamily : org.jetbrains.kotlin.analysis.api.types.KaFunctionTypeFamily {
    /**
     * Whether this family represents reflection function types (`KFunction`, `KSuspendFunction`).
     */
    override val isReflect: Boolean

    /**
     * Whether this family represents suspend function types (`SuspendFunction`, `KSuspendFunction`).
     */
    override val isSuspend: Boolean

    /**
     * Whether function types in this family can be inlined by the compiler.
     *
     * For built-in families, `Function` and `SuspendFunction` are inlinable, while `KFunction` and `KSuspendFunction` are not.
     */
    override val isInlinable: Boolean

    /**
     * The maximum number of parameters supported by function types in this family.
     */
    override val maxArity: Int

    /**
     * Whether function references with a simple function type (e.g., `Function0`, `KFunction0`) can be converted to this family.
     */
    override val supportsConversionFromSimpleFunctionType: Boolean

    /**
     * The class name prefix shared by all types in this family.
     *
     * For example, `"Function"` for the `Function` family, `"SuspendFunction"` for the `SuspendFunction` family.
     */
    override val nameBase: String

    /**
     * Returns the [ClassId] of the function type interface for the given [arity].
     *
     * For example, `classId(2)` on the `Function` family returns the [ClassId] for `kotlin.Function2`.
     */
    override fun classId(arity: Int): ClassId
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.types.KaBuiltinFunctionTypeFamilies] instead.**
 *
 * Provides access to the four built-in [function type families][KaFunctionTypeFamily].
 *
 * @see KaTypeInformationProvider.builtinFunctionTypeFamilies
 */
@KaObsoleteComponentApi
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaBuiltinFunctionTypeFamilies : org.jetbrains.kotlin.analysis.api.types.KaBuiltinFunctionTypeFamilies {
    /**
     * The `Function` family representing regular function types
     * (e.g., `Function0`, `Function1`, ..., `FunctionN`).
     */
    override val function: KaFunctionTypeFamily

    /**
     * The `SuspendFunction` family representing suspend function types
     * (e.g., `SuspendFunction0`, `SuspendFunction1`, ..., `SuspendFunctionN`).
     */
    override val suspendFunction: KaFunctionTypeFamily

    /**
     * The `KFunction` family representing reflection types for regular functions
     * (e.g., `KFunction0`, `KFunction1`, ..., `KFunctionN`).
     */
    override val kFunction: KaFunctionTypeFamily

    /**
     * The `KSuspendFunction` family representing reflection types for suspend functions
     * (e.g., `KSuspendFunction0`, `KSuspendFunction1`, ..., `KSuspendFunctionN`).
     */
    override val kSuspendFunction: KaFunctionTypeFamily
}

/**
 * The object contains [ClassId]s of well known Kotlin types.
 */
@Deprecated(
    message = "Use 'org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds' instead.",
    replaceWith = ReplaceWith("KaStandardTypeClassIds", "org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds"),
)
public object KaStandardTypeClassIds {
    /** The [Unit] class ID. */
    public val UNIT: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.UNIT

    /** The [Int] class ID. */
    public val INT: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.INT

    /** The [Long] class ID. */
    public val LONG: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.LONG

    /** The [Short] class ID. */
    public val SHORT: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.SHORT

    /** The [Byte] class ID. */
    public val BYTE: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.BYTE

    /** The [Float] class ID. */
    public val FLOAT: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.FLOAT

    /** The [Double] class ID. */
    public val DOUBLE: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.DOUBLE

    /** The [Char] class ID. */
    public val CHAR: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.CHAR

    /** The [Boolean] class ID. */
    public val BOOLEAN: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.BOOLEAN

    /** The [String] class ID. */
    public val STRING: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.STRING

    /** The [CharSequence] class ID. */
    public val CHAR_SEQUENCE: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.CHAR_SEQUENCE

    /** The [Any] class ID. */
    public val ANY: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.ANY

    /** The [Nothing] class ID. */
    public val NOTHING: ClassId get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.NOTHING

    /** A set of primitive class IDs. */
    public val PRIMITIVES: Set<ClassId> get() = org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds.PRIMITIVES
}

@Deprecated("Use `KaStandardTypeClassIds` instead", ReplaceWith("KaStandardTypeClassIds"))
@Suppress("DEPRECATION")
public object DefaultTypeClassIds {
    /** The [Unit] class ID. */
    public val UNIT: ClassId get() = KaStandardTypeClassIds.UNIT

    /** The [Int] class ID. */
    public val INT: ClassId get() = KaStandardTypeClassIds.INT

    /** The [Long] class ID. */
    public val LONG: ClassId get() = KaStandardTypeClassIds.LONG

    /** The [Short] class ID. */
    public val SHORT: ClassId get() = KaStandardTypeClassIds.SHORT

    /** The [Byte] class ID. */
    public val BYTE: ClassId get() = KaStandardTypeClassIds.BYTE

    /** The [Float] class ID. */
    public val FLOAT: ClassId get() = KaStandardTypeClassIds.FLOAT

    /** The [Double] class ID. */
    public val DOUBLE: ClassId get() = KaStandardTypeClassIds.DOUBLE

    /** The [Char] class ID. */
    public val CHAR: ClassId get() = KaStandardTypeClassIds.CHAR

    /** The [Boolean] class ID. */
    public val BOOLEAN: ClassId get() = KaStandardTypeClassIds.BOOLEAN

    /** The [String] class ID. */
    public val STRING: ClassId get() = KaStandardTypeClassIds.STRING

    /** The [CharSequence] class ID. */
    public val CHAR_SEQUENCE: ClassId get() = KaStandardTypeClassIds.CHAR_SEQUENCE

    /** The [Any] class ID. */
    public val ANY: ClassId get() = KaStandardTypeClassIds.ANY

    /** The [Nothing] class ID. */
    public val NOTHING: ClassId get() = KaStandardTypeClassIds.NOTHING

    /** A set of primitive class IDs. */
    public val PRIMITIVES: Set<ClassId> get() = KaStandardTypeClassIds.PRIMITIVES
}

/**
 * The [FunctionTypeKind] of the given [KaType], or `null` if the type is not a function type.
 */
@Deprecated("Use 'functionTypeFamily' instead", level = DeprecationLevel.HIDDEN)
@KaExperimentalApi
@KaContextParameterApi
@KaCustomContextParameterBridge
context(session: KaSession)
public val KaType.functionTypeKind: FunctionTypeKind?
    get() {
        @OptIn(KaSessionComponentImplementationDetail::class)
        return KaTypeInformationProvider::class.java.getDeclaredMethod("getFunctionTypeKind", KaType::class.java)
            .invoke(session, this) as FunctionTypeKind?
    }

/**
 * Whether the [KaType] is denotable. A [denotable type](https://kotlinlang.org/spec/type-system.html#type-kinds) can be expressed in
 * Kotlin code, as opposed to being only constructible via compiler type operations (such as type inference).
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isDenotable", "org.jetbrains.kotlin.analysis.api.types.isDenotable"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isDenotable: Boolean
    get() = with(session) { isDenotable }

/**
 * Whether the [KaType] is a [functional interface type](https://kotlinlang.org/docs/fun-interfaces.html), such as [Runnable]. Such
 * types are also known as SAM types.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isFunctionalInterface", "org.jetbrains.kotlin.analysis.api.types.isFunctionalInterface"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isFunctionalInterface: Boolean
    get() = with(session) { isFunctionalInterface }

/**
 * The [function type family][KaFunctionTypeFamily] of the given [KaType], or `null` if the type is not a function type.
 *
 * For example, `(Int) -> String` belongs to the [Function][KaBuiltinFunctionTypeFamilies.function] family,
 * while `suspend () -> Unit` belongs to the [SuspendFunction][KaBuiltinFunctionTypeFamilies.suspendFunction] family.
 *
 * @see KaBuiltinFunctionTypeFamilies
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.functionTypeFamily", "org.jetbrains.kotlin.analysis.api.types.functionTypeFamily"),
)
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KaType.functionTypeFamily: KaFunctionTypeFamily?
    get() = with(session) { functionTypeFamily }

/**
 * Whether the [KaType] is a [kotlin.Function] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isFunctionType", "org.jetbrains.kotlin.analysis.api.types.isFunctionType"),
)
@OptIn(KaExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isFunctionType: Boolean
    get() = with(session) { isFunctionType }

/**
 * Whether the [KaType] is a [kotlin.reflect.KFunction] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isKFunctionType", "org.jetbrains.kotlin.analysis.api.types.isKFunctionType"),
)
@OptIn(KaExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isKFunctionType: Boolean
    get() = with(session) { isKFunctionType }

/**
 * Whether the [KaType] is a [suspend function](https://kotlinlang.org/spec/asynchronous-programming-with-coroutines.html#suspending-functions)
 * type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isSuspendFunctionType", "org.jetbrains.kotlin.analysis.api.types.isSuspendFunctionType"),
)
@OptIn(KaExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isSuspendFunctionType: Boolean
    get() = with(session) { isSuspendFunctionType }

/**
 * Whether the [KaType] is a `KSuspendFunction` type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isKSuspendFunctionType", "org.jetbrains.kotlin.analysis.api.types.isKSuspendFunctionType"),
)
@OptIn(KaExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isKSuspendFunctionType: Boolean
    get() = with(session) { isKSuspendFunctionType }

/**
 * Whether a public value of the [KaType] can potentially be `null`.
 *
 * If a type can be `null`, it means that this type is not a subtype of [Any]. However, it does not mean one can assign `null` to a
 * variable of this type. It may be unknown whether this type can accept `null`.
 *
 * #### Example
 *
 * A public value of type `T : Any?` can potentially be `null`. But one cannot assign `null` to such a variable because the instantiated
 * type may not be nullable.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isNullable", "org.jetbrains.kotlin.analysis.api.types.isNullable"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isNullable: Boolean
    get() = with(session) { isNullable }

/**
 * Whether the [KaType] is explicitly marked as nullable, i.e., is represented as `T?`.
 *
 * Note that this property just reflects the presence of nullability in the type signature,
 * and sometimes [isMarkedNullable] being false doesn't imply that the given type cannot hold `null` or be assigned with it.
 *
 * For example, [isMarkedNullable] doesn't expand type aliases to check the nullability of their underlying type:
 * ```kotlin
 * typealias NonMarkedNullableAlias = String?
 *
 * fun main() {
 *     val x: NonMarkedNullableAlias = null
 * }
 * ```
 * The type of `x` is `NonMarkedNullableAlias`, which is not marked as nullable. However, it still represents a nullable type and can hold `null` and can be assigned with that.
 *
 * To explicitly check whether a type can potentially hold `null`, use [isNullable].
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isMarkedNullable", "org.jetbrains.kotlin.analysis.api.types.isMarkedNullable"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isMarkedNullable: Boolean
    get() = with(session) { isMarkedNullable }

/**
 * Whether the [KaType] is a [org.jetbrains.kotlin.analysis.api.types.KaFlexibleType] / [org.jetbrains.kotlin.analysis.api.types.KaDynamicType] with flexible nullability or [org.jetbrains.kotlin.analysis.api.types.KaErrorType] with unknown nullability.
 * Both safe and ordinary calls are valid on such types.
 *
 * Note that a flexible / dynamic type has a flexible nullability when the lower bound is non-nullable and the upper bound is nullable.
 * E.g. `T!` has `T` as the lower bound and `T?` as the upper bound, hence it has a flexible nullability.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.hasFlexibleNullability", "org.jetbrains.kotlin.analysis.api.types.hasFlexibleNullability"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.hasFlexibleNullability: Boolean
    get() = with(session) { hasFlexibleNullability }

/**
 * Whether the [KaType] is a [Unit] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isUnitType", "org.jetbrains.kotlin.analysis.api.types.isUnitType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isUnitType: Boolean
    get() = with(session) { isUnitType }

/**
 * Whether the [KaType] is an [Int] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isIntType", "org.jetbrains.kotlin.analysis.api.types.isIntType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isIntType: Boolean
    get() = with(session) { isIntType }

/**
 * Whether the [KaType] is a [Long] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isLongType", "org.jetbrains.kotlin.analysis.api.types.isLongType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isLongType: Boolean
    get() = with(session) { isLongType }

/**
 * Whether the [KaType] is a [Short] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isShortType", "org.jetbrains.kotlin.analysis.api.types.isShortType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isShortType: Boolean
    get() = with(session) { isShortType }

/**
 * Whether the [KaType] is a [Byte] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isByteType", "org.jetbrains.kotlin.analysis.api.types.isByteType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isByteType: Boolean
    get() = with(session) { isByteType }

/**
 * Whether the [KaType] is a [Float] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isFloatType", "org.jetbrains.kotlin.analysis.api.types.isFloatType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isFloatType: Boolean
    get() = with(session) { isFloatType }

/**
 * Whether the [KaType] is a [Double] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isDoubleType", "org.jetbrains.kotlin.analysis.api.types.isDoubleType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isDoubleType: Boolean
    get() = with(session) { isDoubleType }

/**
 * Whether the [KaType] is a [Char] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isCharType", "org.jetbrains.kotlin.analysis.api.types.isCharType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isCharType: Boolean
    get() = with(session) { isCharType }

/**
 * Whether the [KaType] is a [Boolean] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isBooleanType", "org.jetbrains.kotlin.analysis.api.types.isBooleanType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isBooleanType: Boolean
    get() = with(session) { isBooleanType }

/**
 * Whether the [KaType] is a [String] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isStringType", "org.jetbrains.kotlin.analysis.api.types.isStringType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isStringType: Boolean
    get() = with(session) { isStringType }

/**
 * Whether the [KaType] is a [CharSequence] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isCharSequenceType", "org.jetbrains.kotlin.analysis.api.types.isCharSequenceType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isCharSequenceType: Boolean
    get() = with(session) { isCharSequenceType }

/**
 * Whether the [KaType] is an [Any] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isAnyType", "org.jetbrains.kotlin.analysis.api.types.isAnyType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isAnyType: Boolean
    get() = with(session) { isAnyType }

/**
 * Whether the [KaType] is a [Nothing] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isNothingType", "org.jetbrains.kotlin.analysis.api.types.isNothingType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isNothingType: Boolean
    get() = with(session) { isNothingType }

/**
 * Whether the [KaType] is a [UInt] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isUIntType", "org.jetbrains.kotlin.analysis.api.types.isUIntType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isUIntType: Boolean
    get() = with(session) { isUIntType }

/**
 * Whether the [KaType] is a [ULong] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isULongType", "org.jetbrains.kotlin.analysis.api.types.isULongType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isULongType: Boolean
    get() = with(session) { isULongType }

/**
 * Whether the [KaType] is a [UShort] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isUShortType", "org.jetbrains.kotlin.analysis.api.types.isUShortType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isUShortType: Boolean
    get() = with(session) { isUShortType }

/**
 * Whether the [KaType] is a [UByte] type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isUByteType", "org.jetbrains.kotlin.analysis.api.types.isUByteType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isUByteType: Boolean
    get() = with(session) { isUByteType }

/**
 * The class symbol backing the given [KaType], if available.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.expandedSymbol", "org.jetbrains.kotlin.analysis.api.types.expandedSymbol"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.expandedSymbol: KaClassSymbol?
    get() = with(session) { expandedSymbol }

/**
 * The type that corresponds to the given [KaType] with fully expanded type aliases.
 *
 * Type aliases are usually expanded immediately by the compiler, so most [KaType]s should already present in their expanded forms.
 * Nonetheless, it is possible to obtain unexpanded types from the Analysis API, and [fullyExpandedType] may be used to expand type
 * aliases in such types.
 *
 * #### Example
 *
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
 *
 * @see KaType.abbreviation
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.fullyExpandedType", "org.jetbrains.kotlin.analysis.api.types.fullyExpandedType"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.fullyExpandedType: KaType
    get() = with(session) { fullyExpandedType }

/**
 * Whether the [KaType] is an array or a primitive array type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isArrayOrPrimitiveArray", "org.jetbrains.kotlin.analysis.api.types.isArrayOrPrimitiveArray"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isArrayOrPrimitiveArray: Boolean
    get() = with(session) { isArrayOrPrimitiveArray }

/**
 * Whether the [KaType] is an array or a primitive array type, and its element is also an array type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isNestedArray", "org.jetbrains.kotlin.analysis.api.types.isNestedArray"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isNestedArray: Boolean
    get() = with(session) { isNestedArray }

/**
 * Checks whether the given [KaType] is a class type with the given [ClassId].
 */
context(session: KaSession)
public fun KaType.isClassType(classId: ClassId): Boolean {
    return with(session) {
        isClassType(
            classId = classId,
        )
    }
}

/**
 * Whether the [KaType] is a primitive type.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.isPrimitive", "org.jetbrains.kotlin.analysis.api.types.isPrimitive"),
)
@KaContextParameterApi
context(session: KaSession)
public val KaType.isPrimitive: Boolean
    get() = with(session) { isPrimitive }

/**
 * The default initializer for the given [KaType], or `null` if the type is neither nullable, a primitive, nor a string.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("this.defaultInitializer", "org.jetbrains.kotlin.analysis.api.types.defaultInitializer"),
)
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KaType.defaultInitializer: String?
    get() = with(session) { defaultInitializer }

/**
 * Provides access to the built-in [function type families][KaFunctionTypeFamily].
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith("builtinFunctionTypeFamilies", "org.jetbrains.kotlin.analysis.api.types.builtinFunctionTypeFamilies"),
)
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val builtinFunctionTypeFamilies: KaBuiltinFunctionTypeFamilies
    get() = with(session) { builtinFunctionTypeFamilies }
