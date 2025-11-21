/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

class BtaV2StrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<Pair<KotlinToolchains, ExecutionPolicy>>> {
            val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
            val v2Args: List<Named<Pair<KotlinToolchains, ExecutionPolicy>>> =
                listOf(
                    Named.named("[v2] in-process", kotlinToolchains to kotlinToolchains.createInProcessExecutionPolicy()),
                    Named.named("[v2] within daemon", kotlinToolchains to kotlinToolchains.createDaemonExecutionPolicy())
                )

            return v2Args
        }
    }
}