/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.utils.exceptions.withSourceEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.syntax.ParseTreeKind
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * Unlike `(SyntaxId) -> Boolean`, [matches] takes a primitive `Int`, so non-inline functions can call it for every visited node
 * without boxing the IDs (some of them are out of the `Integer` cache range).
 */
fun interface SyntaxIdPredicate {
    fun matches(id: SyntaxId): Boolean
}

/**
 * Provides syntax-specific services for the source elements of a [org.jetbrains.kotlin.fir.FirSession].
 *
 * There is one implementation per [ParseTreeKind]; it must match the kind of source elements produced by the raw FIR builder
 * used for the session's sources.
 * The implementation is registered only for source sessions (in the CLI, it's selected by [org.jetbrains.kotlin.cli.parseTreeKind]);
 * the Analysis API always uses the PSI-based implementation.
 */
@NoMutableState
abstract class FirSyntaxComponent<T> : FirSessionComponent {
    abstract val parseTreeKind: ParseTreeKind

    abstract fun KtSourceElement.unwrap(): T

    protected fun unwrapError(actualSourceElement: KtSourceElement): Nothing {
        errorWithAttachment(
            "${this::class.simpleName} can't process ${actualSourceElement::class.simpleName}. " +
                    "The session's parse tree kind must match the raw FIR builder that produced its sources."
        ) {
            withSourceEntry("source", actualSourceElement)
            withEntry("sourceKind", actualSourceElement.kind::class.simpleName)
        }
    }

    /**
     * The name of the node type or token with this [SyntaxId], e.g., `private` or `class`.
     * It's used, for instance, to render modifiers in diagnostics.
     *
     * TODO: KT-91973
     */
    abstract val SyntaxId.name: String

    /**
     * Iterates recursively over the descendants of the element in pre-order, up to the given [depth] or indefinitely if [depth] is -1.
     * The element itself is not visited. With [reverse], the children of each node are visited from last to first.
     *
     * [processChild] is invoked for each descendant whose type satisfies [isTargetType].
     * It returns `true` to continue the iteration or `false` to stop it.
     */
    abstract fun KtSourceElement.forEachChildMatching(
        depth: Int,
        reverse: Boolean,
        isTargetType: SyntaxIdPredicate,
        processChild: (KtSourceElement) -> Boolean,
    )

    /**
     * The tree-independent part of [forEachChildMatching].
     */
    protected inline fun <N> forEachChildMatching(
        root: N,
        depth: Int,
        reverse: Boolean,
        isTargetType: SyntaxIdPredicate,
        getSyntaxId: (N) -> SyntaxId,
        getChildren: (N) -> List<N>,
        processChild: (N) -> Boolean,
    ) {
        val stack = mutableListOf(root to 0)

        while (stack.isNotEmpty()) {
            val [element, currentDepth] = stack.popLast()

            if (currentDepth != 0 && isTargetType.matches(getSyntaxId(element)) && !processChild(element)) {
                return
            }

            if (currentDepth == depth) {
                continue
            }

            getChildren(element).butIf(!reverse) { it.asReversed() }.forEach { child ->
                stack += child to (currentDepth + 1)
            }
        }
    }

    abstract fun KtSourceElement.extractModifierList(): FirModifierList?

    abstract fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement>

    abstract fun KtSourceElement.findFunctionTypeContextParameterListSource(): KtSourceElement?

    abstract fun KtSourceElement.findDefaultValueSource(): KtSourceElement?

    /**
     * Finds the `val` or `var` keyword among the direct children of the element
     * (e.g., of a parameter, a property, a destructuring declaration or its entry).
     */
    abstract fun KtSourceElement.findValOrVarKeywordSource(): KtSourceElement?

    abstract fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement?

    /**
     * Returns whether the operand of this assignment is parenthesized, labeled or annotated.
     *
     * The operand is the left-hand side of an assignment or augmented-assignment operator call (`(x) = ...`, `(x) += ...`), the base of a
     * postfix increment or decrement (`(x)++`), or the operand of a prefix increment or decrement (`++(x)`).
     * This should only be called for a source element corresponding to one of those constructs.
     */
    abstract fun KtSourceElement.hasUnwrappableAsAssignmentLhs(): Boolean

    abstract fun KtSourceElement.findParameterNameSource(name: Name): KtSourceElement?

    abstract fun KtSourceElement.getContainingStringTemplateEntriesCount(): Int?

    abstract fun KtSourceElement.isTrailingLambdaOnNewLine(): Boolean

    /**
     * Returns whether this type reference is the callee of a constructor call,
     * i.e., a supertype is initialized by calling its constructor: `class A : B()`
     *
     * Returns `false` for a supertype without a constructor call (`class A : I`) and for an element that isn't a type reference.
     */
    abstract fun KtSourceElement.isInConstructorCallee(): Boolean

    /**
     * Returns whether this type reference is declared in a `where` clause rather than in the type parameter declaration itself.
     */
    abstract fun KtSourceElement.isInTypeConstraint(): Boolean

    /**
     * Returns the identifier of a reference, a type projection, or the selector of a qualified expression as written in the source
     * (e.g., `_` or `__`), or `null` if it's not applicable to the element.
     */
    abstract fun KtSourceElement.getRawIdentifier(): CharSequence?

    /**
     * Returns the name identifier of this declaration (if applicable) as written in the source.
     */
    abstract fun KtSourceElement.getRawName(): String?

    /**
     * Returns whether this type reference is nullable twice in the source (e.g., `String??`).
     */
    abstract fun KtSourceElement.isRedundantNullable(): Boolean

    /**
     * Returns whether this enum entry has a body in source.
     *
     * Returns `false` if entry has a constructor call, but doesn't have a body:
     *
     * ```kotlin
     * enum class E(i: Int) { FOO(42) }
     * ```
     *
     * We have to go down to source level, since this cannot be checked only by FIR element. This is because in FIR all enum entries
     * with constructor calls have a fake [org.jetbrains.kotlin.fir.declarations.FirEnumEntry.initializer] with an anonymous object,
     * regardless of whether the entry had body originally.
     */
    abstract fun KtSourceElement.hasEnumEntryBody(): Boolean

    /**
     * Returns whether this enum entry has an initializer (a constructor call) in source.
     *
     * Reason of implementing this at source level and not in FIR is same as in [hasEnumEntryBody] method.
     */
    abstract fun KtSourceElement.hasEnumEntryInitializer(): Boolean

    /**
     * Whether the element is a binary expression with an annotated leftmost operand that is placed directly in a statement container,
     * possibly as the leftmost operand of an enclosing chain of binary expressions.
     *
     * The annotation applies only to the leftmost operand, not to the whole expression, as one might expect:
     *
     * ```kotlin
     * fun foo(x: Int, y: Int) {
     *     @Ann x + y * 2 > 0 // `true` for `@Ann x + y * 2`, the leftmost operand of `> 0`
     *     1 + @Ann x         // `false`: the annotated operand is not the leftmost one
     *     val z = @Ann x + y // `false`: not a statement
     * }
     * ```
     */
    abstract fun KtSourceElement.isAnnotatedBinaryExpressionStatement(): Boolean

    /**
     * Finds the delegated super type entry (`Foo by bar`) that directly contains the element.
     */
    abstract fun KtSourceElement.findDelegatedSuperTypeEntrySource(): KtSourceElement?

    /**
     * Invokes [process] for each `is` or binary expression that is the condition expression or the range of a `when` branch.
     * The receiver is a `when` expression with a subject, and [process] gets the condition expression and its operation,
     * which is either [org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds.IS_EXPRESSION] or the ID of the operator token of the binary
     * expression.
     *
     * Parenthesized conditions are skipped:
     *
     * ```kotlin
     * when (x) {
     *     a < b -> {}       // `a < b` with `LT`
     *     in a..b -> {}     // `a..b` with `RANGE`
     *     y is String -> {} // `y is String` with `IS_EXPRESSION`
     *     (a < b) -> {}     // skipped
     * }
     * ```
     */
    abstract fun KtSourceElement.forEachWhenConditionOperation(process: (expression: KtSourceElement, operation: SyntaxId) -> Unit)

    /**
     * Finds the annotations on [typeArgument] of the call if it's an underscore type argument (`_`).
     * Here, the receiver is the call.
     */
    abstract fun KtSourceElement.findUnderscoreTypeArgumentAnnotationSources(typeArgument: KtSourceElement): List<KtSourceElement>

    /**
     * Finds the token glued to the start of the literal if the element is a literal and such a token is prohibited, as in `a foo"bar"`.
     * See `org.jetbrains.kotlin.KtLiteralPrefixAndSuffix`.
     *
     * The implementations for the trees whose parser already reports the problem return `null` (LightTree, KMP)
     */
    abstract fun KtSourceElement.findProhibitedLiteralPrefixSource(): KtSourceElement?

    /**
     * Finds the token glued to the end of the literal if the element is a literal and such a token is prohibited, as in `1in a`.
     * See [findProhibitedLiteralPrefixSource].
     */
    abstract fun KtSourceElement.findProhibitedLiteralSuffixSource(): KtSourceElement?
}

val FirSession.syntaxComponent: FirSyntaxComponent<*> by FirSession.Companion.sessionComponentAccessor()

context(sessionHolder: SessionHolder)
inline fun <R> withSyntax(block: FirSyntaxComponent<*>.() -> R): R = sessionHolder.session.syntaxComponent.block()
