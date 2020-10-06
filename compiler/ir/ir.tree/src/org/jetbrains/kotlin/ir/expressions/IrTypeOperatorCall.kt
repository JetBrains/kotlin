/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType

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
     * Currently this is possible in Kotlin/JVM only, however, there's a big demand for SAM conversion for Kotlin interfaces.
     */
    SAM_CONVERSION,

    /**
     * Implicit dynamic cast: implicit cast from `dynamic` to `T`.
     * This currently can happen in Kotlin/JS only.
     */
    IMPLICIT_DYNAMIC_CAST,

    /**
     * C-like reinterpret_cast<T> using as primitive type operation in JS
     */
    REINTERPRET_CAST;
}

abstract class IrTypeOperatorCall : IrExpression() {
    abstract val operator: IrTypeOperator
    abstract var argument: IrExpression
    abstract var typeOperand: IrType
    abstract val typeOperandClassifier: IrClassifierSymbol
}
