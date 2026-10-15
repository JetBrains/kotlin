/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtDeclarationWithReturnType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction

/**
 * The type of the given [KtExpression], or `null` if it does not have a type.
 *
 * In particular:
 *
 * - A not-null type for valued expressions (e.g., a variable, a function call, a lambda expression).
 * - [Unit] for statements (e.g., assignments, loops).
 * - `null` for [KtExpression]s that are not a part of the expression tree (e.g., expressions in import or package statements).
 *
 * ### Expression vs. expected type
 *
 * The Analysis API distinguishes between an expression's type and its expected type, which represent different aspects of the Kotlin
 * type system.
 *
 * The expression type represents the actual type of an expression after it has been resolved. It reflects the result of type inference,
 * smart casts, and implicit conversions.
 *
 * The expected type represents the type that is expected for an expression at a specific location in the code. This is determined by
 * the context in which the expression appears, such as a variable type for its initializer, or a parameter type for a function call.
 */
context(session: KaSession)
public val KtExpression.expressionType: KaType?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.expressionTypeProvider.expressionType(this)
    }

/**
 * The return type of the given [KtDeclarationWithReturnType], either declared explicitly or inferred.
 *
 * Depending on the declaration kind, the return type is:
 *
 * - The return type for a function, including anonymous functions and function literals (lambdas). A function with a block body and
 *   without a declared return type returns [Unit].
 * - The constructed class type for a constructor, e.g., `Foo<T>` for a constructor of `class Foo<T>`.
 * - The property type for a property and its getter, and [Unit] for its setter.
 * - The field type for an explicit backing field, which may differ from the property type.
 * - The parameter type for a value, context, lambda, `for` loop, `catch`, or function type parameter.
 * - The type of the destructured value for a destructuring declaration, and the component type for its entries.
 * - The enum class type for an enum entry.
 *
 * If the type cannot be resolved or inferred (e.g., it refers to an unresolved class, or implicit types depend on each other
 * recursively), the result is a [KaErrorType][org.jetbrains.kotlin.analysis.api.types.KaErrorType].
 *
 * ### `vararg` parameters
 *
 * For a `vararg foo: T` parameter, the resulting type is the full `Array<out T>` type, or a primitive array type such as `IntArray` for
 * a primitive `T` (unlike [KaValueParameterSymbol.returnType][org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol.returnType],
 * which is `T`).
 *
 * The reasoning behind this is that [KaCallableSymbol.returnType][org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol.returnType]
 * sees the parameter from the declaration's semantic perspective, representing the signature of the parameter, which contains just the
 * element type. In this paradigm, `vararg` arrays are constructed separately under the hood.
 *
 * At the same time, [returnType] represents a use-site perspective, which has to desugar `vararg` parameters because they are consumed
 * as array types.
 *
 * Apart from `vararg` parameters, the result is the same as the return type of the declaration's
 * [KaCallableSymbol][org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol] if there is one (function type parameters have no symbol,
 * and destructuring declarations have a non-callable symbol).
 */
context(session: KaSession)
public val KtDeclarationWithReturnType.returnType: KaType
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.expressionTypeProvider.returnType(this)
    }

/**
 * The function type of the given [KtFunction].
 *
 * For a regular function, the result is a `kotlin.FunctionN<P1, P2, ..., R>` type where:
 *
 * - `N` is the number of value parameters in the function.
 * - `Px` is the type of the x-th value parameter.
 * - `R` is the return type of the function.
 *
 * Depending on the function's attributes, such as `suspend` or reflective access, a different functional type such as
 * `SuspendFunction`, `KFunction`, or `KSuspendFunction` will be constructed.
 */
context(session: KaSession)
public val KtFunction.functionType: KaType
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.expressionTypeProvider.functionType(this)
    }

/**
 * The expected [KaType] for the given [PsiElement] if it is an expression, or `null` if the element does not have an expected type.
 * The expected type represents the type that is expected for an expression at a specific location in the code.
 *
 * See [expressionType] for a discussion about the expression type vs. the expected type.
 */
context(session: KaSession)
public val PsiElement.expectedType: KaType?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.expressionTypeProvider.expectedType(this)
    }

/**
 * Whether this expression is *definitely null*, based on the declared nullability and smart cast types derived from data-flow analysis
 * facts.
 *
 * Only nullability from stable smart casts is considered. See the [smart cast sink stability](https://kotlinlang.org/spec/type-inference.html#smart-cast-sink-stability)
 * section of the Kotlin specification for more information.
 *
 * #### Examples
 *
 * ```
 *   public fun <T : Any> foo(t: T, nt: T?, s: String, ns: String?) {
 *     t     // t.isDefinitelyNull()  == false && t.isDefinitelyNotNull()  == true
 *     nt    // nt.isDefinitelyNull() == false && nt.isDefinitelyNotNull() == false
 *     s     // s.isDefinitelyNull()  == false && s.isDefinitelyNotNull()  == true
 *     ns    // ns.isDefinitelyNull() == false && ns.isDefinitelyNotNull() == false
 *
 *     if (ns != null) {
 *       ns  // ns.isDefinitelyNull() == false && ns.isDefinitelyNotNull() == true
 *     } else {
 *       ns  // ns.isDefinitelyNull() == true  && ns.isDefinitelyNotNull() == false
 *     }
 *
 *     ns!!  // From this point on: ns.isDefinitelyNull() == false && ns.isDefinitelyNotNull() == true
 *   }
 * ```
 */
context(session: KaSession)
public val KtExpression.isDefinitelyNull: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.expressionTypeProvider.isDefinitelyNull(this)
    }

/**
 * Whether this expression is *definitely not null*.
 *
 * @see isDefinitelyNull
 */
context(session: KaSession)
public val KtExpression.isDefinitelyNotNull: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.expressionTypeProvider.isDefinitelyNotNull(this)
    }
