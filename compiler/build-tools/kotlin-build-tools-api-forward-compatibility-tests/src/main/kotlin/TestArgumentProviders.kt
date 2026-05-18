/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.*
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

typealias CompilerExecutionStrategyConfiguration = Pair<KotlinToolchains, ExecutionPolicy>

class DefaultForwardCompatibilityExecutionPolicyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<CompilerExecutionStrategyConfiguration>> {
            val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
            return listOf(
                named(
                    "[${kotlinToolchains.getCompilerVersion()}][daemon]",
                    kotlinToolchains to kotlinToolchains.daemonExecutionPolicy {}
                ),
                named(
                    "[${kotlinToolchains.getCompilerVersion()}][in-process]",
                    kotlinToolchains to kotlinToolchains.createInProcessExecutionPolicy()
                ),
            )
        }
    }
}
