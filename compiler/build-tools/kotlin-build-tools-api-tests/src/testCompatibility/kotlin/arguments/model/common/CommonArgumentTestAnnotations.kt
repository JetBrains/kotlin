/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.common

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * Parameterized test annotation for validating backward compatibility of all common compiler arguments
 * across Build Tools API versions (BTAv1 and BTAv2).
 *
 * This annotation generates test cases for every common compiler argument, testing each argument
 * with both BTA versions to ensure backward compatibility guarantees are maintained.
 *
 * @see AllCommonCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    AllCommonCompilerArgumentsWithBtaVersionsArgumentProvider::class
)
annotation class AllCommonCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for validating backward compatibility of enum-typed common compiler arguments
 * across Build Tools API versions (BTAv1 and BTAv2).
 *
 * This annotation generates test cases specifically for common compiler arguments with enum types,
 * testing each enum argument with both BTA versions to verify backward compatibility.
 *
 * @see EnumCommonCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    EnumCommonCompilerArgumentsWithBtaVersionsArgumentProvider::class
)
annotation class EnumCommonCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for validating backward compatibility of nullable common compiler arguments
 * across Build Tools API versions (BTAv1 and BTAv2).
 *
 * This annotation generates test cases specifically for common compiler arguments with nullable types,
 * testing each nullable argument with both BTA versions to ensure backward compatibility.
 *
 * @see NullableCommonCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    NullableCommonCompilerArgumentsWithBtaVersionsArgumentProvider::class
)
annotation class NullableCommonCompilerArgumentsWithBtaVersionsTest