/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.konan

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * Parameterized test annotation for all Native compiler arguments across Build Tools API versions.
 *
 * @see AllNativeCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(AllNativeCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class AllNativeCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for Native compiler arguments that have invalid typed values
 * (a path containing the platform path separator inside a single search-path entry).
 *
 * @see InvalidArgumentValueNativeCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(InvalidArgumentValueNativeCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class InvalidArgumentValueNativeCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for Native compiler arguments that have invalid raw values,
 * across BTA v2 execution strategies.
 *
 * @see InvalidRawValueNativeCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(InvalidRawValueNativeCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider::class)
annotation class InvalidRawValueNativeCompilerArgumentsBtaV2StrategyAgnosticTest

/**
 * Parameterized test annotation for nullable Native compiler arguments across Build Tools API versions.
 *
 * @see NullableNativeCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(NullableNativeCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class NullableNativeCompilerArgumentsWithBtaVersionsTest
