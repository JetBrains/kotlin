/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.wasm

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(AllWasmCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class AllWasmCompilerArgumentsWithBtaVersionsTest

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(InvalidRawValueWasmCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider::class)
annotation class InvalidRawValueWasmCompilerArgumentsBtaV2StrategyAgnosticTest

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(NullableWasmCompilerArgumentsWithBtaVersionsArgumentProvider::class)
annotation class NullableWasmCompilerArgumentsWithBtaVersionsTest
