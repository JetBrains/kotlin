/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationTarget
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration

/**
 * The deprecation status of the given symbol, or `null` if the symbol is not deprecated.
 *
 * This considers deprecation annotations applied to the symbol itself and, for property-related
 * symbols, deprecation annotations with appropriate use-site targets.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaSymbol.deprecation: KaDeprecation?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolInformationProvider.deprecation(this)
    }

/**
 * Whether the symbol is deprecated at any level.
 *
 * This is a convenience property equivalent to `deprecation != null`.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaSymbol.isDeprecated: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolInformationProvider.isDeprecated(this)
    }

/**
 * Whether the function symbol meets all the requirements to be declared as an [operator function](https://kotlinlang.org/docs/operator-overloading.html).
 *
 * In Kotlin, the set of functions which can be declared as an operator is predefined. [canBeOperator] not only checks the name of a
 * potential operator function, but also its signature, depending on the operator.
 *
 * [canBeOperator] does not determine whether the function symbol *is* declared as an operator. For this purpose, use
 * [KaNamedFunctionSymbol.isOperator] instead.
 *
 * #### Example
 *
 * ```kotlin
 * class A
 *
 * fun A.plus(that: A): A = A() // canBeOperator = true, as it meets all requirements for `plus`.
 *
 * operator fun A.contains(that: A): Boolean = true // canBeOperator = true, as it's already an operator.
 *
 * fun A.something(that: A): A = A() // canBeOperator = false, as there is no operator with such a name.
 *
 * fun A.minus(): A = A() // canBeOperator = false, as `minus` is a binary operator and should have one parameter.
 * ```
 */
@KaExperimentalApi
context(session: KaSession)
public val KaNamedFunctionSymbol.canBeOperator: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolInformationProvider.canBeOperator(this)
    }

/**
 * A set of applicable targets for an annotation class symbol, or `null` if the symbol is not an annotation class.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaClassSymbol.applicableAnnotationTargets: Set<KaAnnotationTarget>?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolInformationProvider.applicableAnnotationTargets(this)
    }

/**
 * Whether the property is an [inline property](https://kotlinlang.org/docs/inline-functions.html#inline-properties).
 * A property is considered `inline` when both of its accessors are `inline` or when it has the `inline` keyword.
 * The `inline` keyword on a property is syntactic sugar for marking both accessors as `inline`.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaKotlinPropertySymbol.isInline: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolInformationProvider.isInline(this)
    }

/**
 * A [FqName] which can be used to import the given symbol, or `null` if the symbol cannot be imported.
 */
@KaIdeApi
context(session: KaSession)
public val KaSymbol.importableFqName: FqName?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolInformationProvider.importableFqName(this)
    }

/**
 * A set of annotation targets matching for the given symbol, or `null` if the symbol cannot be annotated.
 * Annotations with one of targets from the returned set can be placed on the [KaSymbol] without an explicit use-site target.
 *
 * Such as, annotations with [KaAnnotationTarget.PROPERTY] or [KaAnnotationTarget.VALUE_PARAMETER] can be directly placed on
 * a 'val' value parameter, while annotations for [KaAnnotationTarget.PROPERTY_GETTER] and [KaAnnotationTarget.PROPERTY_SETTER]
 * can only be applied using the `@set:AnnotationName` syntax.
 *
 * Check the [Annotation use-site targets](https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets) documentation
 * for additional information.
 */
@KaIdeApi
@KaExperimentalApi
context(session: KaSession)
public val KaSymbol.defaultAnnotationTargets: Set<KaAnnotationTarget>?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolInformationProvider.defaultAnnotationTargets(this)
    }

/**
 * The return value status of the function (should it be used, or can it be ignored).
 * See the [KEEP](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0412-unused-return-value-checker.md) for details.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaNamedFunctionSymbol.returnValueStatus: KaReturnValueStatus
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolInformationProvider.returnValueStatus(this)
    }

/**
 * File-level annotations (`@file:SomeAnnotation`) of the source file this top-level [KaDeclarationSymbol] was defined in,
 * or `null` for a nested declaration.
 *
 * This API is only intended to be used by the TypeScript export utility.
 */
@KaNonPublicApi
context(session: KaSession)
public val KaDeclarationSymbol.containingFileAnnotations: KaAnnotationList?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolInformationProvider.containingFileAnnotations(this)
    }

/**
 * The source file name for the given [KtDeclaration] located in a Kotlin library (klib), or `null` if the declaration is not located in
 * a klib, or when the source file name is not available.
 */
@KaNonPublicApi
context(session: KaSession)
public val KaDeclarationSymbol.klibSourceFileName: String?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.sourceProvider.klibSourceFileName(this)
    }
