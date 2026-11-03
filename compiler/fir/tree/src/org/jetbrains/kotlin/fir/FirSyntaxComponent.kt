/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.ParserMode
import org.jetbrains.kotlin.name.Name

/**
 * Provides syntax-specific services for the source elements of a [FirSession].
 *
 * There is one implementation per [ParserMode]; it must match the kind of source elements produced by the raw FIR builder
 * used for the session's sources.
 * The implementation is registered only for source sessions (in the CLI, it's selected by [org.jetbrains.kotlin.config.parserMode]);
 * the Analysis API always uses the PSI-based implementation.
 *
 * The API of this class must not expose IntelliJ platform types,
 * since the [ParserMode.KmpTree]-based implementation is intended to be free of the IntelliJ platform dependency.
 */
@NoMutableState
abstract class FirSyntaxComponent : FirSessionComponent {
    abstract val parserMode: ParserMode

    /**
     * Finds the context parameter lists (`context(...)`) in the modifier list of the declaration.
     */
    abstract fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement>

    /**
     * Finds the context parameter list of the function type, which is possibly wrapped into a type reference or a nullable type.
     */
    abstract fun KtSourceElement.findFunctionTypeContextParameterListSource(): KtSourceElement?

    /**
     * Finds the default value of the value parameter.
     */
    abstract fun KtSourceElement.findDefaultValueSource(): KtSourceElement?

    /**
     * Finds the `val` or `var` keyword of the value parameter or property.
     */
    abstract fun KtSourceElement.findValOrVarKeywordSource(): KtSourceElement?

    /**
     * Finds the explicit receiver of the dot-qualified expression.
     */
    abstract fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement?

    /**
     * Finds the name identifier of the parameter called [name] inside the element.
     */
    abstract fun KtSourceElement.findParameterNameSource(name: Name): KtSourceElement?

    /**
     * Returns the number of entries of the closest string template containing the element,
     * or `null` if the element is not inside a string template.
     */
    abstract fun KtSourceElement.getContainingStringTemplateEntriesCount(): Int?

    /**
     * Whether the element is a trailing lambda separated from the rest of its call by a line break.
     */
    abstract fun KtSourceElement.isTrailingLambdaOnNewLine(): Boolean
}

val FirSession.syntaxComponent: FirSyntaxComponent by FirSession.sessionComponentAccessor()
