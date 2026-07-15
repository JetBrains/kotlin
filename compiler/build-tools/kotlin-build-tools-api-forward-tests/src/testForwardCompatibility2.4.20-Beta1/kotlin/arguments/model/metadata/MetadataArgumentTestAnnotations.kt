/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.metadata

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * Parameterized test annotation for all Metadata compiler arguments across Build Tools API versions.
 *
 * @see AllMetadataCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(AllMetadataCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class AllMetadataCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for Metadata compiler arguments that have invalid typed values
 * (a path containing the platform path separator inside a single search-path entry).
 *
 * @see InvalidArgumentValueMetadataCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(InvalidArgumentValueMetadataCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class InvalidArgumentValueMetadataCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for nullable Metadata compiler arguments across Build Tools API versions.
 *
 * @see NullableMetadataCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(NullableMetadataCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class NullableMetadataCompilerArgumentsWithBtaVersionsTest

/**
 * Parameterized test annotation for Metadata compiler arguments whose raw CLI value must be rejected
 * (e.g. a non-existent enum entry), checked at compilation time under both BTAv2 execution strategies.
 *
 * @see InvalidRawValueMetadataCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider
 */
/**
 * Parameterized test annotation for Metadata compiler arguments whose raw CLI value must be rejected,
 * across Build Tools API versions (conversion-level, no compilation).
 *
 * @see InvalidRawValueMetadataCompilerArgumentsWithBtaVersionsArgumentProvider
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(InvalidRawValueMetadataCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class InvalidRawValueMetadataCompilerArgumentsWithBtaVersionsTest

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(InvalidRawValueMetadataCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider::class)
annotation class InvalidRawValueMetadataCompilerArgumentsBtaV2StrategyAgnosticTest
