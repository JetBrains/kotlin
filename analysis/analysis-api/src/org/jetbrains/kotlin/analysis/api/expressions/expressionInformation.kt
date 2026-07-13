/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.expressions

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.resolution.KaContextSensitiveResolutionStatus
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

/**
 * Checks if the [KtSimpleNameExpression] is an implicit reference to a companion object via the containing class.
 *
 * #### Example
 *
 * ```
 * class A {
 *    companion object {
 *       fun foo() {}
 *    }
 * }
 * ```
 *
 * Given a call `A.foo()`, `A` is an implicit reference to the companion object, so `isImplicitReferenceToCompanion` returns `true`.
 */
context(session: KaSession)
public val KtSimpleNameExpression.isImplicitReferenceToCompanion: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.resolver.isImplicitReferenceToCompanion(this)
    }

/**
 * The [context-sensitive resolution](https://github.com/Kotlin/KEEP/issues/379) status of the [KtSimpleNameExpression]:
 * whether the name is already resolved through context-sensitive resolution, and whether a redundant explicit
 * qualifier or import could be removed in favor of it.
 *
 * The information is available even when the `-Xcontext-sensitive-resolution` feature is not enabled.
 *
 * #### Example
 *
 * ```
 * enum class Foo { BAR }
 *
 * fun usage(): Foo {
 *     return Foo.BAR // the 'Foo.' qualifier can be removed -> KaContextSensitiveResolutionStatus.QualifierCanBeRemoved
 * }
 * ```
 *
 * @see KaContextSensitiveResolutionStatus
 */
@KaExperimentalApi
context(session: KaSession)
public val KtSimpleNameExpression.contextSensitiveResolutionStatus: KaContextSensitiveResolutionStatus
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.resolver.contextSensitiveResolutionStatus(this)
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
context(session: KaSession)
public val KtExpression.isUsedAsExpression: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.expressionInformationProvider.isUsedAsExpression(this)
    }

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
context(session: KaSession)
public val KtExpression.isUsedAsResultOfLambda: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.expressionInformationProvider.isUsedAsResultOfLambda(this)
    }

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
context(session: KaSession)
public val KtExpression.isStableForSmartCasting: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.expressionInformationProvider.isStableForSmartCasting(this)
    }
