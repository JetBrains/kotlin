/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

class BtaVersionsCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<KotlinToolchains>> {
            return buildList {
                val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)


                add(
                    named(
                        "[v2][${kotlinToolchains.getCompilerVersion()}]",
                        kotlinToolchains
                    )
                )

            }
        }
    }
}
