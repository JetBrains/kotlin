/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaExpressionInformationProvider : KaSessionComponent {
    /**
     * The [symbol][KaCallableSymbol] of the callable which the given [KtReturnExpression] returns from.
     */
    @Deprecated("The API is obsolete. Use `resolveSymbol()` instead.", ReplaceWith("resolveSymbol()"))
    @KaIdeApi
    public val KtReturnExpression.targetSymbol: KaCallableSymbol?

    /**
     * Computes the missing cases of the given [KtWhenExpression].
     *
     * The computed missing cases are not affected by the existence or absence of an `else` branch.
     *
     * #### Example
     *
     * In the following code, `Direction.WEST` and `Direction.EAST` are missing branches:
     *
     * ```
     * enum class Direction {
     *   NORTH, SOUTH, WEST, EAST
     * }
     *
     * foo = when(direction) {
     *   Direction.NORTH -> 1
     *   Direction.SOUTH -> 2
     *   else -> 3
     * }
     * ```
     *
     * If the [KtWhenExpression] has no subject, then the `else` is reported as missing even if it is explicitly present:
     *
     * ```
     * fun test() {
     *     when {
     *         true -> {}
     *         else -> {}
     *     }
     * }
     * ```
     */
    @KaIdeApi
    public fun KtWhenExpression.computeMissingCases(): List<KaWhenMissingCase>

    /**
     * Whether the value of the given [KtExpression] is used. In other words, `true` if the value of the expression is not safe to discard.
     *
     * [isUsedAsExpression] performs a **conservative check** instead of exhaustive control-flow analysis. For example, `x` in the following
     * example *is possibly used*, even though the value is never consumed at runtime: `x + try { throw Exception() } finally { return }`.
     *
     * #### Example
     *
     * In the following examples, `x` is used as a value (`x.isUsedAsExpression == true`):
     *
     *   - `if (x) { ... } else { ... }`
     *   - `val a = x`
     *   - `x + 8`
     *   - `when (x) { 1 -> ...; else -> ... }`
     *
     * In these expressions, `x` is not used as a value (`x.isUsedAsExpression == false`)
     *
     *   - `run { x; println(50) }`
     *   - `when (x) { else -> ... }`
     */
    public val KtExpression.isUsedAsExpression: Boolean

    /**
     * Whether the value of the given [KtExpression] is used as the resulting expression of some lambda block.
     *
     * Note that [isUsedAsResultOfLambda] performs a **conservative check** instead of exhaustive control-flow analysis and
     * `isUsedAsResultOfLambda` being `true` doesn't imply that the containing lambda itself is used.
     *
     * It's also vital to not confuse lambda expressions with regular scope blocks (like `if` branches).
     * #### Example
     *
     * In the following examples, `x` is used as a result of a lambda (`x.isUsedAsResultOfLambda == true`):
     *
     *   - `{ x -> println(0); x }`
     *   - `{ { x }; 5 }`
     *
     * In these expressions, `x` is not used as a result of a lambda (`x.isUsedAsResultOfLambda == false`)
     *
     *   - `{ x -> println(0); x + 1 }`
     *   - `{ x; println(50) }`
     *   - `{ if (true) { x } else { x } }`
     *   - `fun(x: Int) = x`
     */
    @KaExperimentalApi
    public val KtExpression.isUsedAsResultOfLambda: Boolean

    /**
     * Whether this expression can be used as a stable smart-cast sink at the current program point.
     *
     * Stability is only one prerequisite for applying a smart cast. A `true` result does not mean that the expression is
     * currently smart-cast to a more specific type. It only means that data-flow facts about this expression may be applied
     * if such facts are available.
     *
     * This property is meaningful for expressions that can be represented as data-flow variables, such as local variables,
     * parameters, receivers, and property accesses. It returns `false` for expressions that are not smart-cast sinks, such
     * as literals, function calls, and other types of expressions.
     *
     * Stable smart-cast sinks generally include:
     *
     *   - Local `val`s, value parameters, and stable receivers.
     *   - Local `var`s that the compiler can treat as effectively immutable at this program point.
     *   - `val` properties without delegation or custom getters, accessed through a stable receiver and declared in the
     *     current module.
     *
     * Unstable smart-cast sinks generally include:
     *
     *   - Non-local `var` properties.
     *   - Delegated properties.
     *   - Properties with custom getters.
     *   - Properties from separately compiled modules.
     *   - `open` properties whose receiver is not known to have a final type.
     *   - Local `var`s whose captured writes may invalidate the relevant data-flow facts.
     *
     * See the Kotlin specification section on
     * [smart cast sink stability](https://kotlinlang.org/spec/type-inference.html#smart-cast-sink-stability).
     *
     * #### Example
     *
     * ```kotlin
     * class Container(val value: Any?)
     *
     * open class OpenContainer(open val value: Any?)
     *
     * fun source(): Any? = ""
     *
     * fun test(container: Container, openContainer: OpenContainer) {
     *     val local: Any? = ""
     *     var mutableLocal: Any? = ""
     *     mutableLocal = "tracked assignment"
     *
     *     // local.isStableForSmartCasting == true
     *     // mutableLocal.isStableForSmartCasting == true
     *     // container.value.isStableForSmartCasting == true
     *     // openContainer.value.isStableForSmartCasting == false
     *     // source().isStableForSmartCasting == false
     * }
     * ```
     */
    @KaExperimentalApi
    public val KtExpression.isStableForSmartCasting: Boolean
}

/**
 * Represents a missing case in a `when` expression.
 *
 * @see KaExpressionInformationProvider.computeMissingCases
 */
@KaIdeApi
public sealed class KaWhenMissingCase {
    /**
     * Represents a missing check for an `expect` declaration.
     * Because `actual` types may define more cases (e.g., additional enum values), exhaustiveness checks are explicitly disabled for those.
     * Also see KT-20306.
     */
    @KaIdeApi
    public sealed class ExpectTypeCase : KaWhenMissingCase() {
        /**
         * Represents a missing check for an `expect sealed class`.
         */
        @KaIdeApi
        public object ExpectSealedClassCase : ExpectTypeCase()

        /**
         * Represents a missing check for an `expect sealed interface`.
         */
        @KaIdeApi
        public object ExpectSealedInterfaceCase : ExpectTypeCase()

        /**
         * Represents a missing check for an `expect enum class`.
         */
        @KaIdeApi
        public object ExpectEnumCase : ExpectTypeCase()
    }

    /**
     * Represents a missing `null` check.
     */
    @KaIdeApi
    public object NullCase : KaWhenMissingCase()

    /**
     * Represents a missing boolean check.
     *
     * @property value the boolean value that is missing.
     */
    @KaIdeApi
    public class BooleanCase(public val value: Boolean) : KaWhenMissingCase()

    /**
     * Represents a missing type check.
     *
     * @property classId the class id of the type that is missing.
     * @property isObject whether the type is an object. For objects, the `is` keyword is redundant.
     * @property ownTypeParameterCount the number of type parameters of the type that is missing.
     */
    @KaIdeApi
    public class TypeCase(
        public val classId: ClassId,
        public val isObject: Boolean,
        public val ownTypeParameterCount: Int,
    ) : KaWhenMissingCase()

    /**
     * Represents a missing enum entry check.
     *
     * @property callableId the callable id of the enum entry that is missing.
     */
    @KaIdeApi
    public class EnumEntryCase(
        public val callableId: CallableId
    ) : KaWhenMissingCase()

    /**
     * Represents a missing case other than those mentioned above.
     */
    @KaIdeApi
    public object UnknownCase : KaWhenMissingCase()
}

/**
 * The [symbol][KaCallableSymbol] of the callable which the given [KtReturnExpression] returns from.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@Deprecated("The API is obsolete. Use `resolveSymbol()` instead.", ReplaceWith("resolveSymbol()"))
@KaIdeApi
@KaContextParameterApi
context(session: KaSession)
public val KtReturnExpression.targetSymbol: KaCallableSymbol?
    @Suppress("DEPRECATION")
    get() = with(session) { targetSymbol }

/**
 * Computes the missing cases of the given [KtWhenExpression].
 *
 * The computed missing cases are not affected by the existence or absence of an `else` branch.
 *
 * #### Example
 *
 * In the following code, `Direction.WEST` and `Direction.EAST` are missing branches:
 *
 * ```
 * enum class Direction {
 *   NORTH, SOUTH, WEST, EAST
 * }
 *
 * foo = when(direction) {
 *   Direction.NORTH -> 1
 *   Direction.SOUTH -> 2
 *   else -> 3
 * }
 * ```
 *
 * If the [KtWhenExpression] has no subject, then the `else` is reported as missing even if it is explicitly present:
 *
 * ```
 * fun test() {
 *     when {
 *         true -> {}
 *         else -> {}
 *     }
 * }
 * ```
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaIdeApi
@KaContextParameterApi
context(session: KaSession)
public fun KtWhenExpression.computeMissingCases(): List<KaWhenMissingCase> {
    return with(session) {
        computeMissingCases()
    }
}

/**
 * Whether the value of the given [KtExpression] is used. In other words, `true` if the value of the expression is not safe to discard.
 *
 * [isUsedAsExpression] performs a **conservative check** instead of exhaustive control-flow analysis. For example, `x` in the following
 * example *is possibly used*, even though the value is never consumed at runtime: `x + try { throw Exception() } finally { return }`.
 *
 * #### Example
 *
 * In the following examples, `x` is used as a value (`x.isUsedAsExpression == true`):
 *
 *   - `if (x) { ... } else { ... }`
 *   - `val a = x`
 *   - `x + 8`
 *   - `when (x) { 1 -> ...; else -> ... }`
 *
 * In these expressions, `x` is not used as a value (`x.isUsedAsExpression == false`)
 *
 *   - `run { x; println(50) }`
 *   - `when (x) { else -> ... }`
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(session: KaSession)
public val KtExpression.isUsedAsExpression: Boolean
    get() = with(session) { isUsedAsExpression }

/**
 * Whether the value of the given [KtExpression] is used as the resulting expression of some lambda block.
 *
 * Note that [isUsedAsResultOfLambda] performs a **conservative check** instead of exhaustive control-flow analysis and
 * `isUsedAsResultOfLambda` being `true` doesn't imply that the containing lambda itself is used.
 *
 * It's also vital to not confuse lambda expressions with regular scope blocks (like `if` branches).
 * #### Example
 *
 * In the following examples, `x` is used as a result of a lambda (`x.isUsedAsResultOfLambda == true`):
 *
 *   - `{ x -> println(0); x }`
 *   - `{ { x }; 5 }`
 *
 * In these expressions, `x` is not used as a result of a lambda (`x.isUsedAsResultOfLambda == false`)
 *
 *   - `{ x -> println(0); x + 1 }`
 *   - `{ x; println(50) }`
 *   - `{ if (true) { x } else { x } }`
 *   - `fun(x: Int) = x`
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KtExpression.isUsedAsResultOfLambda: Boolean
    get() = with(session) { isUsedAsResultOfLambda }

/**
 * Whether this expression can be used as a stable smart-cast sink at the current program point.
 *
 * Stability is only one prerequisite for applying a smart cast. A `true` result does not mean that the expression is
 * currently smart-cast to a more specific type. It only means that data-flow facts about this expression may be applied
 * if such facts are available.
 *
 * This property is meaningful for expressions that can be represented as data-flow variables, such as local variables,
 * parameters, receivers, and property accesses. It returns `false` for expressions that are not smart-cast sinks, such
 * as literals, function calls, and other types of expressions.
 *
 * Stable smart-cast sinks generally include:
 *
 *   - Local `val`s, value parameters, and stable receivers.
 *   - Local `var`s that the compiler can treat as effectively immutable at this program point.
 *   - `val` properties without delegation or custom getters, accessed through a stable receiver and declared in the
 *     current module.
 *
 * Unstable smart-cast sinks generally include:
 *
 *   - Non-local `var` properties.
 *   - Delegated properties.
 *   - Properties with custom getters.
 *   - Properties from separately compiled modules.
 *   - `open` properties whose receiver is not known to have a final type.
 *   - Local `var`s whose captured writes may invalidate the relevant data-flow facts.
 *
 * See the Kotlin specification section on
 * [smart cast sink stability](https://kotlinlang.org/spec/type-inference.html#smart-cast-sink-stability).
 *
 * #### Example
 *
 * ```kotlin
 * class Container(val value: Any?)
 *
 * open class OpenContainer(open val value: Any?)
 *
 * fun source(): Any? = ""
 *
 * fun test(container: Container, openContainer: OpenContainer) {
 *     val local: Any? = ""
 *     var mutableLocal: Any? = ""
 *     mutableLocal = "tracked assignment"
 *
 *     // local.isStableForSmartCasting == true
 *     // mutableLocal.isStableForSmartCasting == true
 *     // container.value.isStableForSmartCasting == true
 *     // openContainer.value.isStableForSmartCasting == false
 *     // source().isStableForSmartCasting == false
 * }
 * ```
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KtExpression.isStableForSmartCasting: Boolean
    get() = with(session) { isStableForSmartCasting }
