/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.runner

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream
import kotlin.streams.asStream

class BuildRunnerProvider(val strategy: ExecutionStrategy) {

    operator fun invoke(project: Project): BuildRunner {
        val runner = BuildRunner(project)
        when (strategy) {
            ExecutionStrategy.DAEMON -> runner.useDaemonStrategy(emptyList())
            ExecutionStrategy.IN_PROCESS -> runner.useInProcessStrategy()
        }
        return runner
    }

    override fun toString() = strategy.toString()
}

class CompilationTestArgumentsProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return setOf(
            BuildRunnerProvider(ExecutionStrategy.DAEMON),
            BuildRunnerProvider(ExecutionStrategy.IN_PROCESS),
        ).asSequence().map { Arguments.of(it) }.asStream()
    }
}