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

    abstract fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement>

    abstract fun KtSourceElement.findFunctionTypeContextParameterListSource(): KtSourceElement?

    abstract fun KtSourceElement.findDefaultValueSource(): KtSourceElement?

    abstract fun KtSourceElement.findValOrVarKeywordSource(): KtSourceElement?

    abstract fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement?

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
