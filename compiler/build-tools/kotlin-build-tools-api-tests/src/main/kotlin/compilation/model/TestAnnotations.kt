/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * Annotation for parameterized tests that evaluate compilation behavior using different configuration strategies.
 * This involves the matrix (BTAv1, BTAv2) x (daemon, in-process)
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    DefaultStrategyAgnosticCompilationTestArgumentProvider::class
)
annotation class DefaultStrategyAgnosticCompilationTest

/**
 * Annotation for parameterized tests that evaluate compilation behavior using only BTAv2
 * This involves the variants: daemon, in-process
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(
    BtaV2StrategyAgnosticCompilationTestArgumentProvider::class
)
annotation class BtaV2StrategyAgnosticCompilationTest