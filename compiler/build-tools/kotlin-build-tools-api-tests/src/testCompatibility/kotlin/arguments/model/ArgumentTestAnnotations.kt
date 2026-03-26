/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * Parameterized test annotation for validating backward compatibility of all JVM compiler arguments
 * across Build Tools API versions (BTAv1 and BTAv2).
 *
 * This annotation generates test cases for every JVM compiler argument, testing each argument
 * with both BTA versions to ensure backward compatibility guarantees are maintained.
 *
 * @see AllJvmCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    AllJvmCompilerArgumentsWithBtaVersionsArgumentProvider::class
)
annotation class AllJvmCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for validating backward compatibility of enum-typed JVM compiler arguments
 * across Build Tools API versions (BTAv1 and BTAv2).
 *
 * This annotation generates test cases specifically for JVM compiler arguments with enum types,
 * testing each enum argument with both BTA versions to verify backward compatibility.
 *
 * @see EnumJvmCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    EnumJvmCompilerArgumentsWithBtaVersionsArgumentProvider::class
)
annotation class EnumJvmCompilerArgumentsWithBtaVersionsTest


/**
 * Parameterized test annotation for validating backward compatibility of nullable JVM compiler arguments
 * across Build Tools API versions (BTAv1 and BTAv2).
 *
 * This annotation generates test cases specifically for JVM compiler arguments with nullable types,
 * testing each nullable argument with both BTA versions to ensure backward compatibility.
 *
 * @see NullableJvmCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    NullableJvmCompilerArgumentsWithBtaVersionsArgumentProvider::class
)
annotation class NullableJvmCompilerArgumentsWithBtaVersionsTest