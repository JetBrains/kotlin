/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol

/**
 * Whether the [KaType] is denotable. A [denotable type](https://kotlinlang.org/spec/type-system.html#type-kinds) can be expressed in
 * Kotlin code, as opposed to being only constructible via compiler type operations (such as type inference).
 */
context(session: KaSession)
public val KaType.isDenotable: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isDenotable(this)
    }

/**
 * Whether the [KaType] is a [functional interface type](https://kotlinlang.org/docs/fun-interfaces.html), such as [Runnable]. Such
 * types are also known as SAM types.
 */
context(session: KaSession)
public val KaType.isFunctionalInterface: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isFunctionalInterface(this)
    }

/**
 * The [function type family][KaFunctionTypeFamily] of the given [KaType], or `null` if the type is not a function type.
 *
 * For example, `(Int) -> String` belongs to the [Function][KaBuiltinFunctionTypeFamilies.function] family,
 * while `suspend () -> Unit` belongs to the [SuspendFunction][KaBuiltinFunctionTypeFamilies.suspendFunction] family.
 *
 * @see KaBuiltinFunctionTypeFamilies
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.functionTypeFamily: KaFunctionTypeFamily?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.functionTypeFamily(this)
    }

/**
 * Whether the [KaType] is a [kotlin.Function] type.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.isFunctionType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isFunctionType(this)
    }

/**
 * Whether the [KaType] is a [kotlin.reflect.KFunction] type.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.isKFunctionType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isKFunctionType(this)
    }

/**
 * Whether the [KaType] is a [suspend function](https://kotlinlang.org/spec/asynchronous-programming-with-coroutines.html#suspending-functions)
 * type.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.isSuspendFunctionType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isSuspendFunctionType(this)
    }

/**
 * Whether the [KaType] is a `KSuspendFunction` type.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.isKSuspendFunctionType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isKSuspendFunctionType(this)
    }

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
context(session: KaSession)
public val KaType.isNullable: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isNullable(this)
    }

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
context(session: KaSession)
public val KaType.isMarkedNullable: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isMarkedNullable(this)
    }

/**
 * Whether the [KaType] is a [org.jetbrains.kotlin.analysis.api.types.KaFlexibleType] / [org.jetbrains.kotlin.analysis.api.types.KaDynamicType] with flexible nullability or [org.jetbrains.kotlin.analysis.api.types.KaErrorType] with unknown nullability.
 * Both safe and ordinary calls are valid on such types.
 *
 * Note that a flexible / dynamic type has a flexible nullability when the lower bound is non-nullable and the upper bound is nullable.
 * E.g. `T!` has `T` as the lower bound and `T?` as the upper bound, hence it has a flexible nullability.
 */
context(session: KaSession)
public val KaType.hasFlexibleNullability: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.hasFlexibleNullability(this)
    }

/**
 * Whether the [KaType] is a [Unit] type.
 */
context(session: KaSession)
public val KaType.isUnitType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isUnitType(this)
    }

/**
 * Whether the [KaType] is an [Int] type.
 */
context(session: KaSession)
public val KaType.isIntType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isIntType(this)
    }

/**
 * Whether the [KaType] is a [Long] type.
 */
context(session: KaSession)
public val KaType.isLongType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isLongType(this)
    }

/**
 * Whether the [KaType] is a [Short] type.
 */
context(session: KaSession)
public val KaType.isShortType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isShortType(this)
    }

/**
 * Whether the [KaType] is a [Byte] type.
 */
context(session: KaSession)
public val KaType.isByteType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isByteType(this)
    }

/**
 * Whether the [KaType] is a [Float] type.
 */
context(session: KaSession)
public val KaType.isFloatType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isFloatType(this)
    }

/**
 * Whether the [KaType] is a [Double] type.
 */
context(session: KaSession)
public val KaType.isDoubleType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isDoubleType(this)
    }

/**
 * Whether the [KaType] is a [Char] type.
 */
context(session: KaSession)
public val KaType.isCharType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isCharType(this)
    }

/**
 * Whether the [KaType] is a [Boolean] type.
 */
context(session: KaSession)
public val KaType.isBooleanType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isBooleanType(this)
    }

/**
 * Whether the [KaType] is a [String] type.
 */
context(session: KaSession)
public val KaType.isStringType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isStringType(this)
    }

/**
 * Whether the [KaType] is a [CharSequence] type.
 */
context(session: KaSession)
public val KaType.isCharSequenceType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isCharSequenceType(this)
    }

/**
 * Whether the [KaType] is an [Any] type.
 */
context(session: KaSession)
public val KaType.isAnyType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isAnyType(this)
    }

/**
 * Whether the [KaType] is a [Nothing] type.
 */
context(session: KaSession)
public val KaType.isNothingType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isNothingType(this)
    }

/**
 * Whether the [KaType] is a [UInt] type.
 */
context(session: KaSession)
public val KaType.isUIntType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isUIntType(this)
    }

/**
 * Whether the [KaType] is a [ULong] type.
 */
context(session: KaSession)
public val KaType.isULongType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isULongType(this)
    }

/**
 * Whether the [KaType] is a [UShort] type.
 */
context(session: KaSession)
public val KaType.isUShortType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isUShortType(this)
    }

/**
 * Whether the [KaType] is a [UByte] type.
 */
context(session: KaSession)
public val KaType.isUByteType: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isUByteType(this)
    }

/**
 * The class symbol backing the given [KaType], if available.
 */
context(session: KaSession)
public val KaType.expandedSymbol: KaClassSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.expandedSymbol(this)
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
context(session: KaSession)
public val KaType.fullyExpandedType: KaType
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.fullyExpandedType(this)
    }

/**
 * Whether the [KaType] is an array or a primitive array type.
 */
context(session: KaSession)
public val KaType.isArrayOrPrimitiveArray: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isArrayOrPrimitiveArray(this)
    }

/**
 * Whether the [KaType] is an array or a primitive array type, and its element is also an array type.
 */
context(session: KaSession)
public val KaType.isNestedArray: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isNestedArray(this)
    }

/**
 * Whether the [KaType] is a primitive type.
 */
context(session: KaSession)
public val KaType.isPrimitive: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.isPrimitive(this)
    }

/**
 * The default initializer for the given [KaType], or `null` if the type is neither nullable, a primitive, nor a string.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.defaultInitializer: String?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.defaultInitializer(this)
    }

/**
 * Provides access to the built-in [function type families][KaFunctionTypeFamily].
 */
@KaExperimentalApi
context(session: KaSession)
public val builtinFunctionTypeFamilies: KaBuiltinFunctionTypeFamilies
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeInformationProvider.builtinFunctionTypeFamilies()
    }
