/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithReturnType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction

@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaExpressionTypeProvider : KaSessionComponent {
    /**
     * The type of the given [KtExpression], or `null` if it does not have a type.
     *
     * Particularly, the method returns:
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
    public val KtExpression.expressionType: KaType?

    /**
     * The return type of the given [KtDeclarationWithReturnType].
     *
     * Note: For a `vararg foo: T` parameter, the resulting type is the full `Array<out T>` type (unlike
     * [KaValueParameterSymbol.returnType][org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol.returnType],
     * which returns `T`).
     *
     * The reasoning behind this is that [KaCallableSymbol.returnType] sees the parameter from the declaration's semantic perspective,
     * representing the signature of the parameter, which contains just the element type. In this paradigm, `vararg` arrays are
     * constructed separately under the hood.
     *
     * At the same time, [KtDeclaration.returnType][org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider.returnType]
     * from [KaExpressionTypeProvider][org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider.returnType] represents a
     * use-site perspective, which has to desugar `vararg` parameters because they are consumed as array types.
     */
    public val KtDeclarationWithReturnType.returnType: KaType

    /**
     * The return type of the given [KtDeclaration].
     *
     * Note: this property throws an exception if the declaration _can't_ have a return type
     * (i.e., it is not a [KtDeclarationWithReturnType]).
     */
    @Deprecated("Use `KtDeclarationWithReturnType.returnType` overload instead")
    public val KtDeclaration.returnType: KaType
        get() = (this as KtDeclarationWithReturnType).returnType

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
    public val KtFunction.functionType: KaType

    /**
     * The expected [KaType] for the given [PsiElement] if it is an expression, or `null` if the element does not have an expected type.
     * The expected type represents the type that is expected for an expression at a specific location in the code.
     *
     * See [expressionType] for a discussion about the expression type vs. the expected type.
     */
    public val PsiElement.expectedType: KaType?

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
    public val KtExpression.isDefinitelyNull: Boolean

    /**
     * Whether this expression is *definitely not null*.
     *
     * @see isDefinitelyNull
     */
    public val KtExpression.isDefinitelyNotNull: Boolean
}

/**
 * @see KaExpressionTypeProvider.expressionType
 */
@KaContextParameterApi
context(context: KaExpressionTypeProvider)
public val KtExpression.expressionType: KaType?
    get() = with(context) { expressionType }

/**
 * @see KaExpressionTypeProvider.returnType
 */
@KaContextParameterApi
context(context: KaExpressionTypeProvider)
public val KtDeclarationWithReturnType.returnType: KaType
    get() = with(context) { returnType }

/**
 * @see KaExpressionTypeProvider.returnType
 */
@KaContextParameterApi
@Deprecated("Use `KtDeclarationWithReturnType.returnType` overload instead")
context(context: KaExpressionTypeProvider)
public val KtDeclaration.returnType: KaType
    get() = with(context) {
        (this@returnType as KtDeclarationWithReturnType).returnType
    }

/**
 * @see KaExpressionTypeProvider.functionType
 */
@KaContextParameterApi
context(context: KaExpressionTypeProvider)
public val KtFunction.functionType: KaType
    get() = with(context) { functionType }

/**
 * @see KaExpressionTypeProvider.expectedType
 */
@KaContextParameterApi
context(context: KaExpressionTypeProvider)
public val PsiElement.expectedType: KaType?
    get() = with(context) { expectedType }

/**
 * @see KaExpressionTypeProvider.isDefinitelyNull
 */
@KaContextParameterApi
context(context: KaExpressionTypeProvider)
public val KtExpression.isDefinitelyNull: Boolean
    get() = with(context) { isDefinitelyNull }

/**
 * @see KaExpressionTypeProvider.isDefinitelyNotNull
 */
@KaContextParameterApi
context(context: KaExpressionTypeProvider)
public val KtExpression.isDefinitelyNotNull: Boolean
    get() = with(context) { isDefinitelyNotNull }
