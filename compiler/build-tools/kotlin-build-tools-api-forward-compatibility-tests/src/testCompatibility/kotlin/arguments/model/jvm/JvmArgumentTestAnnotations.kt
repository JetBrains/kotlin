/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * Parameterized test annotation for validating forward compatibility of all JVM compiler arguments.
 *
 * This annotation generates test cases for every JVM compiler argument to ensure
 * forward compatibility guarantees are maintained.
 *
 * @see AllJvmCompilerArgumentsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    AllJvmCompilerArgumentsArgumentProvider::class
)
annotation class AllJvmCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for validating forward compatibility of JVM compiler arguments
 * that reject invalid argument values.
 *
 * This annotation generates test cases specifically for JVM compiler arguments where setting
 * an invalid argument value should throw [org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException].
 *
 * @see InvalidArgumentValueJvmCompilerArgumentsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    InvalidArgumentValueJvmCompilerArgumentsArgumentProvider::class
)
annotation class InvalidArgumentValueJvmCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for validating forward compatibility of JVM compiler arguments
 * that reject invalid string values.
 *
 * This annotation generates test cases specifically for JVM compiler arguments where setting
 * an invalid string value should throw [org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException].
 *
 * @see InvalidRawValueJvmCompilerArgumentsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    InvalidRawValueJvmCompilerArgumentsArgumentProvider::class
)
annotation class InvalidRawValueJvmCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for validating forward compatibility of JVM compiler arguments
 * that reject invalid string values, running against both in-process and daemon execution policies.
 *
 * @see InvalidRawValueJvmCompilerArgumentsStrategyAgnosticArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    InvalidRawValueJvmCompilerArgumentsStrategyAgnosticArgumentProvider::class
)
annotation class InvalidRawValueJvmCompilerArgumentsStrategyAgnosticTest
