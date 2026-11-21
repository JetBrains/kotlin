/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.ParseTreeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.utils.exceptions.withSourceEntry
import org.jetbrains.kotlin.name.Name
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

    abstract fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement>

    abstract fun KtSourceElement.findFunctionTypeContextParameterListSource(): KtSourceElement?

    abstract fun KtSourceElement.findDefaultValueSource(): KtSourceElement?

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
}

val FirSession.syntaxComponent: FirSyntaxComponent<*> by FirSession.Companion.sessionComponentAccessor()

context(sessionHolder: SessionHolder)
inline fun <R> withSyntax(block: FirSyntaxComponent<*>.() -> R): R = sessionHolder.session.syntaxComponent.block()
