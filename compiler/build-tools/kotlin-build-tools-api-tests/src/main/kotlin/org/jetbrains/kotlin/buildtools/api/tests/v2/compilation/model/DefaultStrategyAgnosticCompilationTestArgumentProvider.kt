/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.v2.compilation.model

import org.jetbrains.kotlin.buildtools.api.tests.BaseTest
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream
import kotlin.streams.asStream

class DefaultStrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return sequenceOf(
            named("in-process", ExecutionPolicy.ExecutionMode.IN_PROCESS),
            named("within daemon", ExecutionPolicy.ExecutionMode.DAEMON),
        ).map { Arguments.of(it) }.asStream()
    }
}