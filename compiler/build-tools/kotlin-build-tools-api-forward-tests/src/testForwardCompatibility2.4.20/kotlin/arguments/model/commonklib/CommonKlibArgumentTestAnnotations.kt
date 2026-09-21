/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.commonklib

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(AllCommonKlibCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class AllCommonKlibCompilerArgumentsWithBtaVersionsTest

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(InvalidArgumentValueCommonKlibCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class InvalidArgumentValueCommonKlibCompilerArgumentsWithBtaVersionsTest

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(InvalidRawValueCommonKlibCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider::class)
annotation class InvalidRawValueCommonKlibCompilerArgumentsBtaV2StrategyAgnosticTest

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(NullableCommonKlibCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class NullableCommonKlibCompilerArgumentsWithBtaVersionsTest
