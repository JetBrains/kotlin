/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

/**
 * Marks an API as an implementation detail of the Analysis API. Such APIs are not intended to be used outside the implementation of the
 * Analysis API and have no compatibility guarantees.
 *
 * While the Analysis API generally strives to hide implementation details from users, due to architectural reasons, some implementation
 * details cannot be moved out of the API surface for now.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which should not be used outside the Analysis API implementation modules as it does not have any compatibility guarantees")
public annotation class KaImplementationDetail

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
public annotation class KaNonPublicApi

/**
 * Marks an API as designed for and internal to the Kotlin IntelliJ plugin. The API is not intended for public user consumption and does not
 * have any compatibility guarantees.
 *
 * The motivation behind [KaIdeApi] is the following: in a few cases, implementing functionality in the Analysis API is more efficient or
 * more straightforward than implementing it in the Kotlin IntelliJ plugin. The resulting API is normally too specific to be considered a
 * general, public part of the Analysis API.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which is used only from the IntelliJ Kotlin plugin. Such an API should not be used in other places since it has no compatibility guarantees")
public annotation class KaIdeApi

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
public annotation class KaExperimentalApi

/**
 * Marks an API intended for Analysis API implementations & platforms. The API is neither stable nor intended for user consumption.
 *
 * Only declarations inside the user-facing part of the Analysis API (`analysis-api` module) require this opt-in annotation. Platform
 * interface services defined in `analysis-api-platform-interface` are not annotated with [KaPlatformInterface].
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("An API intended for Analysis API implementations & platforms. The API is neither stable nor intended for user consumption.")
public annotation class KaPlatformInterface
