/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression

@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaDataFlowProvider : KaSessionComponent {
    /**
     * [Smart cast information][KaSmartCastInfo] for the given [KtExpression], or `null` if smart casts are not applied to it.
     */
    public val KtExpression.smartCastInfo: KaSmartCastInfo?

    /**
     * The list of [implicit receiver smart casts][KaImplicitReceiverSmartCast] which have refined the expression's implicit receivers to a
     * more specific type. These smart casts are required for the expression to be evaluated. The list does not include smart casts for
     * explicit receivers.
     *
     * #### Example
     *
     * ```kotlin
     * if (this is String) {
     *   this.substring()   // 'this' receiver is explicit, so there is no implicit smart cast here.
     *
     *   smartcast()        // 'this' receiver is implicit, therefore there is an implicit smart cast involved.
     * }
     * ```
     */
    @KaNonPublicApi
    public val KtExpression.implicitReceiverSmartCasts: Collection<KaImplicitReceiverSmartCast>

    @KaNonPublicApi
    public fun computeExitPointSnapshot(statements: List<KtExpression>): KaDataFlowExitPointSnapshot
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.dataflow.KaSmartCastInfo] instead.**
 *
 * Represents smart cast information for an expression.
 */
@KaObsoleteComponentApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSmartCastInfo : org.jetbrains.kotlin.analysis.api.dataflow.KaSmartCastInfo {

    /**
     * Whether the smart cast is [stable](https://kotlinlang.org/spec/type-inference.html#smart-cast-sink-stability).
     */
    public override val isStable: Boolean

    /**
     * The original type of the expression before the smart cast was applied.
     */
    @KaExperimentalApi
    public override val originalType: KaType

    /**
     * The type with the smart cast applied.
     */
    public override val smartCastType: KaType
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.dataflow.KaImplicitReceiverSmartCast] instead.**
 *
 * Represents type information about an implicit receiver which has been smart-cast to a more specific type. An implicit smart cast is
 * applied to an implicit receiver, such as `substring()` called on an implicit `this` given an earlier smart cast `this is String`.
 */
@KaObsoleteComponentApi
@KaNonPublicApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaImplicitReceiverSmartCast : KaLifetimeOwner {
    /**
     * The receiver type with the smart cast applied.
     */
    public val type: KaType

    /**
     * The kind of implicit receiver, i.e. a dispatch or extension receiver.
     */
    public val kind: KaImplicitReceiverSmartCastKind
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.dataflow.KaImplicitReceiverSmartCastKind] instead.**
 *
 * Represents the kind of implicit receiver affected by the smart cast.
 */
@KaObsoleteComponentApi
@KaNonPublicApi
public enum class KaImplicitReceiverSmartCastKind {
    /**
     * The cast is applied to the receiver of a member call.
     */
    DISPATCH,

    /**
     * The cast is applied to the receiver of an extension function or property call.
     */
    EXTENSION,
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.dataflow.KaDataFlowExitPointSnapshot] instead.**
 */
@KaObsoleteComponentApi
@KaNonPublicApi
public class KaDataFlowExitPointSnapshot(
    /**
     * A default expression, if any.
     * @see [DefaultExpressionInfo] for more information.
     */
    public val defaultExpressionInfo: DefaultExpressionInfo?,

    /**
     * A list of expressions that return a value.
     *
     * The expressions are not necessarily [KtReturnExpression]s.
     * For instance, implicit return from a lambda can be an arbitrary expression.
     */
    public val valuedReturnExpressions: List<KtExpression>,

    /**
     * A common supertype of values in [valuedReturnExpressions].
     */
    public val returnValueType: KaType?,

    /**
     * All jump expressions.
     * @see [hasJumps] for the definition of jumps.
     */
    public val jumpExpressions: List<KtExpression>,

    /**
     * `true` if there are any control-flow statements that jump outside given statements.
     * Jumps include both loop jumps (`break` and `continue`) and `return`s.
     * Conditional blocks (`if`) and `throw`s are not considered as jumps.
     */
    public val hasJumps: Boolean,

    /**
     * `true` if next-executed instructions for the potential default expression and jump expressions are different.
     */
    public val hasEscapingJumps: Boolean,

    /**
     * `true` if there are jumps of different kinds (e.g., there is both a `break` and a `return`).
     */
    public val hasMultipleJumpKinds: Boolean,

    /**
     * `true` if two or more jumps have different next-executed instructions.
     * Such as, there are both inner and outer loop `break`, or a `break` and `continue` for the same loop.
     */
    public val hasMultipleJumpTargets: Boolean,

    /**
     * local variable reassignments found in given statements.
     */
    public val variableReassignments: List<VariableReassignment>,
) {
    /**
     * Represents a default expression (generally, a last given statement if it has a meaningful result type).
     * Expressions that always return [Nothing], such as `return`, `break`, `continue` or `throw`, cannot be default expressions.
     */
    @KaNonPublicApi
    public class DefaultExpressionInfo(
        /** The default expression. */
        public val expression: KtExpression,

        /** The default expression type. */
        public val type: KaType,
    )

    /**
     * Represents a local variable reassignment.
     */
    @KaNonPublicApi
    public class VariableReassignment(
        /** The reassignment expression. */
        public val expression: KtExpression,

        /** Reassigned variable symbol. */
        public val variable: KaVariableSymbol,

        /** `true` if the variable is both read and set (as in `x += y` or `x++`). */
        public val isAugmented: Boolean,
    )
}

/**
 * [Smart cast information][KaSmartCastInfo] for the given [KtExpression], or `null` if smart casts are not applied to it.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.dataflow' endpoint instead.",
    replaceWith = ReplaceWith(
        "this.smartCastInfo",
        "org.jetbrains.kotlin.analysis.api.dataflow.smartCastInfo",
    ),
)
@KaContextParameterApi
context(session: KaSession)
public val KtExpression.smartCastInfo: KaSmartCastInfo?
    get() = with(session) { smartCastInfo }

/**
 * The list of [implicit receiver smart casts][KaImplicitReceiverSmartCast] which have refined the expression's implicit receivers to a
 * more specific type. These smart casts are required for the expression to be evaluated. The list does not include smart casts for
 * explicit receivers.
 *
 * #### Example
 *
 * ```kotlin
 * if (this is String) {
 *   this.substring()   // 'this' receiver is explicit, so there is no implicit smart cast here.
 *
 *   smartcast()        // 'this' receiver is implicit, therefore there is an implicit smart cast involved.
 * }
 * ```
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.dataflow' endpoint instead.",
    replaceWith = ReplaceWith(
        "this.implicitReceiverSmartCasts",
        "org.jetbrains.kotlin.analysis.api.dataflow.implicitReceiverSmartCasts",
    ),
)
@KaNonPublicApi
@KaContextParameterApi
context(session: KaSession)
public val KtExpression.implicitReceiverSmartCasts: Collection<KaImplicitReceiverSmartCast>
    get() = with(session) { implicitReceiverSmartCasts }

@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.dataflow' endpoint instead.",
    replaceWith = ReplaceWith(
        "computeExitPointSnapshot(statements)",
        "org.jetbrains.kotlin.analysis.api.dataflow.computeExitPointSnapshot",
    ),
)
@KaNonPublicApi
@KaContextParameterApi
context(session: KaSession)
public fun computeExitPointSnapshot(statements: List<KtExpression>): KaDataFlowExitPointSnapshot {
    return with(session) {
        computeExitPointSnapshot(
            statements = statements,
        )
    }
}
