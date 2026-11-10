/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.ParserMode
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
 * Provides syntax-specific services for the source elements of a [org.jetbrains.kotlin.fir.FirSession].
 *
 * There is one implementation per [ParserMode]; it must match the kind of source elements produced by the raw FIR builder
 * used for the session's sources.
 * The implementation is registered only for source sessions (in the CLI, it's selected by [org.jetbrains.kotlin.config.parserMode]);
 * the Analysis API always uses the PSI-based implementation.
 */
@NoMutableState
abstract class FirSyntaxComponent<T> : FirSessionComponent {
    abstract val parserMode: ParserMode

    abstract fun KtSourceElement.unwrap(): T

    protected fun unwrapError(actualSourceElement: KtSourceElement): Nothing {
        errorWithAttachment(
            "${this::class.simpleName} (parser mode $parserMode) can't process ${actualSourceElement::class.simpleName}. " +
                    "The session's parser mode must match the raw FIR builder that produced its sources."
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
        isTargetType: (SyntaxId) -> Boolean,
        processChild: (KtSourceElement) -> Boolean,
    )

    /**
     * The tree-independent part of [forEachChildMatching].
     */
    protected inline fun <N> forEachChildMatching(
        root: N,
        depth: Int,
        reverse: Boolean,
        isTargetType: (SyntaxId) -> Boolean,
        getSyntaxId: (N) -> SyntaxId,
        getChildren: (N) -> List<N>,
        processChild: (N) -> Boolean,
    ) {
        val stack = mutableListOf(root to 0)

        while (stack.isNotEmpty()) {
            val [element, currentDepth] = stack.popLast()

            if (currentDepth != 0 && isTargetType(getSyntaxId(element)) && !processChild(element)) {
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

    abstract fun KtSourceElement.findParameterNameSource(name: Name): KtSourceElement?

    abstract fun KtSourceElement.getContainingStringTemplateEntriesCount(): Int?

    abstract fun KtSourceElement.isTrailingLambdaOnNewLine(): Boolean
}

val FirSession.syntaxComponent: FirSyntaxComponent<*> by FirSession.Companion.sessionComponentAccessor()

context(sessionHolder: SessionHolder)
inline fun <R> withSyntax(block: FirSyntaxComponent<*>.() -> R): R = sessionHolder.session.syntaxComponent.block()
