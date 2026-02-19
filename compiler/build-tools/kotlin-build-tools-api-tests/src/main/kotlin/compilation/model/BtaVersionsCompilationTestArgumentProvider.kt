/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
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
                val kotlinToolchainV1Adapter =
                    @Suppress("DEPRECATION_ERROR")
                    if (KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) <= KotlinToolingVersion(2, 4, 255, null)) {
                        val asKotlinToolchainsMethod =
                            btaClassloader.loadClass("org.jetbrains.kotlin.buildtools.internal.compat.KotlinToolchainsV1AdapterKt")
                                .getDeclaredMethod("asKotlinToolchains", org.jetbrains.kotlin.buildtools.api.CompilationService::class.java)
                        asKotlinToolchainsMethod.invoke(null, org.jetbrains.kotlin.buildtools.api.CompilationService.loadImplementation(btaClassloader)) as KotlinToolchains
                    } else null
                if (kotlinToolchainV1Adapter != null) {
                    add(
                        named(
                            "[v1][${kotlinToolchainV1Adapter.getCompilerVersion()}]",
                            kotlinToolchainV1Adapter
                        )
                    )
                }
                if (kotlinToolchainV1Adapter == null || kotlinToolchainV1Adapter::class != kotlinToolchains::class) {
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
}
