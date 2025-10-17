/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.internal.compat.asKotlinToolchains
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

class DefaultStrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<Pair<KotlinToolchains, ExecutionPolicy>>> {
            val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
            val kotlinToolchainV1Adapter =
                CompilationService.loadImplementation(BaseCompilationTest::class.java.classLoader).asKotlinToolchains()
            val v1Args: List<Named<Pair<KotlinToolchains, ExecutionPolicy>>> = listOf(
                named("[v1] in-process", kotlinToolchainV1Adapter to kotlinToolchainV1Adapter.createInProcessExecutionPolicy()),
                named("[v1] within daemon", kotlinToolchainV1Adapter to kotlinToolchainV1Adapter.createDaemonExecutionPolicy())
            )
            val v2Args: List<Named<Pair<KotlinToolchains, ExecutionPolicy>>> =
                if (kotlinToolchainV1Adapter::class == kotlinToolchains::class) {
                    // BTA v2 was not available on the classpath and `kotlinToolchain` is actually the fallback KotlinToolchainV1Adapter
                    // we don't want to run the same thing twice, so we don't create arguments for v2
                    emptyList()
                } else {
                    listOf(
                        named("[v2] in-process", kotlinToolchains to kotlinToolchains.createInProcessExecutionPolicy()),
                        named("[v2] within daemon", kotlinToolchains to kotlinToolchains.createDaemonExecutionPolicy())
                    )
                }

            return v1Args + v2Args
        }
    }
}