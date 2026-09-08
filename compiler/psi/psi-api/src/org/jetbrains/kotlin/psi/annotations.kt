/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

/**
 * Functions and classes annotated with [KtPsiInconsistencyHandling] are not intended for general-purpose use, but for working with possibly
 * inconsistent PSI. The specific circumstances need to be described in the documentation of the annotated function/class.
 *
 * Inconsistent PSI cannot be produced by the Kotlin parser. It occurs rarely, for example during modification of the PSI by the IDE. In
 * general, it can be assumed that all PSI is consistent. Inconsistent PSI should only be assumed when there is sufficient proof.
 */
@RequiresOptIn
annotation class KtPsiInconsistencyHandling

/**
 * Marks an API as an implementation detail of the Kotlin PSI API.
 * Such APIs are not intended to be used outside the implementation of the PSI API and have no compatibility guarantees.
 */
@RequiresOptIn("Internal API which should not be used outside the Kotlin PSI API implementation modules as it does not have any compatibility guarantees")
annotation class KtImplementationDetail

/**
 * Marks an API as internal to projects developed by JetBrains. Such APIs are not intended for public user consumption and have less strict
 * compatibility guarantees. For example, a change to the API might be implemented without a deprecation cycle.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which is used in projects developed by JetBrains")
annotation class KtNonPublicApi

/**
 * Marks an API as designed for and internal to the Kotlin IntelliJ plugin. The API is not intended for public user consumption and does not
 * have any compatibility guarantees.
 *
 * The motivation behind [KtIdeApi] is the following: in a few cases, implementing functionality in the Kotlin PSI API is more efficient or
 * more straightforward than implementing it in the Kotlin IntelliJ plugin. The resulting API is normally too specific to be considered a
 * general, public part of the PSI API.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which is used only from the IntelliJ Kotlin plugin. Such an API should not be used in other places since it has no compatibility guarantees")
annotation class KtIdeApi

/**
 * Marks an API as experimental. The API is intended for user consumption, but it's not stable and might change at any moment, or even be
 * removed, without a deprecation cycle.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Experimental API with no compatibility guarantees")
annotation class KtExperimentalApi

/**
 * Marks an API intended for the Analysis API engine and its platform implementations.
 *
 * Unlike the rest of the PSI API, such declarations are not about reading or modifying a syntax tree. They form a contract with the code
 * that builds and hosts the Kotlin PSI — stub building, decompilation, element type registration, indexing. The Kotlin PSI has no platform
 * layer of its own, so the counterpart of such an API always lives in the Analysis API or one of its platforms.
 *
 * The API is neither stable nor intended for consumption by PSI users.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("An API intended for the Analysis API engine and its platform implementations. The API is neither stable nor intended for consumption by PSI users.")
annotation class KtPlatformInterface

/**
 * Marks a class designed as a service provider interface.
 *
 * Apply this to classes that are intended to be subclassed or implemented by external clients.
 *
 * The class members that are intended to be implemented by clients and are not directly accessible should be marked with [KtSpiExtensionPoint].
 *
 * @see KtSpiExtensionPoint
 */
@Target(AnnotationTarget.CLASS)
internal annotation class KtSpi

/**
 * Marks an API as a service provider interface extension point. Such APIs are designed to be implemented, not called directly. There are no
 * compatibility guarantees for usage of these APIs, only for their implementation.
 *
 * @see KtSpi
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@RequiresOptIn("An API designed for implementation only. Direct usage has no compatibility guarantees.")
annotation class KtSpiExtensionPoint
