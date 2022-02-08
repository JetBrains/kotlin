/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

enum class IrTypeOperator {
    /** Explicit cast: `e as Type` */
    CAST,

    /** Implicit cast: value of type `A` is used where a value of type `B` is expected */
    IMPLICIT_CAST,

    /** Implicit cast from a value of nullability flexible type `A!` to non-null type `B`, `B :> A` */
    IMPLICIT_NOTNULL,

    /** Implicit coercion to Unit: expression of type `A, !(A <: kotlin.Unit)` is used where `kotlin.Unit` is expected */
    IMPLICIT_COERCION_TO_UNIT,

    /**
     * Implicit integer coercion: expression of integer type `A` (`kotlin.Int`, `kotlin.Byte`, ...)
     * is used where another integer type `B` (`kotlin.Int`, `kotlin.Byte`, ...) is expected.
     * This mostly happens for constant expressions.
     */
    IMPLICIT_INTEGER_COERCION,

    /** Safe cast: `e as? Type` */
    SAFE_CAST,

    /** Instance-of check: `a is Type` */
    INSTANCEOF,

    /** Instance-of check: `a !is Type` */
    NOT_INSTANCEOF, // TODO drop and replace with `INSTANCEOF<T>(x).not()`?

    /**
     * SAM conversion: value of functional type F is used where Single Abstract Method interface value is expected.
     */
    SAM_CONVERSION,

    /**
     * Implicit dynamic cast: implicit cast from `dynamic` to `T`.
     * This currently can happen in Kotlin/JS only.
     */
    IMPLICIT_DYNAMIC_CAST,

    /**
     * C-like reinterpret_cast<T> using as primitive type operation in JS.
     * On JVM, tells back-end to treat argument as a value of a given type (even though exact JVM types might differ).
     */
    REINTERPRET_CAST;
}
