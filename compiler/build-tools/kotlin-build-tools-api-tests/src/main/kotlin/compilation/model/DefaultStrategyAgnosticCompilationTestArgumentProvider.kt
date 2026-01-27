/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.internal.compat.asKotlinToolchains
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
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
            return buildList {
                val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
                val kotlinToolchainV1Adapter =
                    if (KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) < KotlinToolingVersion(2, 4, 0, null)) {
                        @Suppress("DEPRECATION_ERROR")
                        org.jetbrains.kotlin.buildtools.api.CompilationService.loadImplementation(BaseCompilationTest::class.java.classLoader).asKotlinToolchains()
                    } else null
                if (kotlinToolchainV1Adapter != null) {
                    add(
                        named(
                            "[v1][${kotlinToolchainV1Adapter.getCompilerVersion()}] in-process",
                            kotlinToolchainV1Adapter to kotlinToolchainV1Adapter.createInProcessExecutionPolicy()
                        )
                    )
                    add(
                        named(
                            "[v1][${kotlinToolchainV1Adapter.getCompilerVersion()}] within daemon",
                            kotlinToolchainV1Adapter to kotlinToolchainV1Adapter.daemonExecutionPolicyBuilder().build()
                        )
                    )
                }
                if (kotlinToolchainV1Adapter == null || kotlinToolchainV1Adapter::class != kotlinToolchains::class) {
                    // only add BTA v2 when `kotlinToolchains` is not actually a v1 adapter
                    // we don't want to run the same thing twice, we want to test the real v2 implementation
                    add(
                        named(
                            "[v2][${kotlinToolchains.getCompilerVersion()}] in-process",
                            kotlinToolchains to kotlinToolchains.createInProcessExecutionPolicy()
                        )
                    )
                    add(
                        named(
                            "[v2][${kotlinToolchains.getCompilerVersion()}] within daemon",
                            kotlinToolchains to kotlinToolchains.daemonExecutionPolicyBuilder().build()
                        )
                    )
                }
            }
        }
    }
}