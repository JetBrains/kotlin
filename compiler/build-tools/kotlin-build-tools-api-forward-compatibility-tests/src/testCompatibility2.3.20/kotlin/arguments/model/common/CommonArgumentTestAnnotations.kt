/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.common

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * Parameterized test annotation for validating forward compatibility of all common compiler arguments.
 *
 * This annotation generates test cases for every common compiler argument to ensure
 * forward compatibility guarantees are maintained.
 *
 * @see AllCommonCompilerArgumentsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    AllCommonCompilerArgumentsArgumentProvider::class
)
annotation class AllCommonCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for validating forward compatibility of common compiler arguments
 * that reject invalid argument values.
 *
 * This annotation generates test cases specifically for common compiler arguments where setting
 * an invalid value should throw [org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException].
 *
 * @see InvalidArgumentValueCommonCompilerArgumentsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    InvalidArgumentValueCommonCompilerArgumentsArgumentProvider::class
)
annotation class InvalidArgumentValueCommonCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for validating forward compatibility of common compiler arguments
 * that reject invalid string values.
 *
 * This annotation generates test cases specifically for common compiler arguments where setting
 * an invalid string value should throw [org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException].
 *
 * @see InvalidRawValueCommonCompilerArgumentsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    InvalidRawValueCommonCompilerArgumentsArgumentProvider::class
)
annotation class InvalidRawValueCommonCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for validating forward compatibility of common compiler arguments
 * that reject invalid string values, running against both in-process and daemon execution policies.
 *
 * @see InvalidRawValueCommonCompilerArgumentsStrategyAgnosticArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    InvalidRawValueCommonCompilerArgumentsStrategyAgnosticArgumentProvider::class
)
annotation class InvalidRawValueCommonCompilerArgumentsStrategyAgnosticTest
