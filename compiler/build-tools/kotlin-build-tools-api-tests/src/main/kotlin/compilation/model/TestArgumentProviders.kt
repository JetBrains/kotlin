/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.Scenario
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jsScenario
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
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
            val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
            val v2Args: List<Named<Pair<KotlinToolchains, ExecutionPolicy>>> = listOf(
                named(
                    "[v2][${kotlinToolchains.getCompilerVersion()}][in-process]",
                    kotlinToolchains to kotlinToolchains.createInProcessExecutionPolicy()
                ), named(
                    "[v2][${kotlinToolchains.getCompilerVersion()}][daemon]",
                    kotlinToolchains to kotlinToolchains.daemonExecutionPolicyBuilder().build()
                )
            )

            return v2Args
        }
    }
}

class BtaV2StrategyAndPlatformAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<ProjectCreator>> {
            return BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments().flatMap { executionStrategyConfiguration ->
                listOfNotNull(
                    named(
                        "${executionStrategyConfiguration.name}[JVM]"
                    ) { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                        baseTest.jvmProject(executionStrategyConfiguration.payload, testAction)
                    }, if (executionStrategyConfiguration.payload.first.supportsJs()) {
                        named(
                            "${executionStrategyConfiguration.name}[JS]"
                        ) { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                            baseTest.jsProject(executionStrategyConfiguration.payload, testAction)
                        }
                    } else null)
            }
        }
    }
}

class DefaultStrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<Pair<KotlinToolchains, ExecutionPolicy>>> {
            return BtaVersionsCompilationTestArgumentProvider.namedStrategyArguments().flatMap { namedArgument ->
                listOf(
                    named(
                        "${namedArgument.name}[in-process]", namedArgument.payload to namedArgument.payload.createInProcessExecutionPolicy()
                    ), named("${namedArgument.name}[daemon]", namedArgument.payload to namedArgument.payload.daemonExecutionPolicy())
                )
            }
        }
    }
}

class DefaultStrategyAndPlatformAgnosticProjectTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<ProjectCreator>> {
            return DefaultStrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
                .flatMap { executionStrategyConfiguration ->
                    listOfNotNull(
                        named(
                            "${executionStrategyConfiguration.name}[JVM]"
                        ) { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                            baseTest.jvmProject(executionStrategyConfiguration.payload, testAction)
                        }, if (executionStrategyConfiguration.payload.first.supportsJs()) {
                            named(
                                "${executionStrategyConfiguration.name}[JS]"
                            ) { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                                baseTest.jsProject(executionStrategyConfiguration.payload, testAction)
                            }
                        } else null)
                }
        }
    }
}


class DefaultStrategyAndPlatformAgnosticScenarioTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<ScenarioCreator>> {
            return DefaultStrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
                .flatMap { executionStrategyConfiguration ->
                    listOfNotNull(
                        named(
                            "${executionStrategyConfiguration.name}[JVM]"
                        ) { baseTest: BaseCompilationTest, testAction: ScenarioAction ->
                            baseTest.jvmScenario(executionStrategyConfiguration.payload, testAction)
                        }, if (executionStrategyConfiguration.payload.first.supportsJs()) {
                            named(
                                "${executionStrategyConfiguration.name}[JS]"
                            ) { baseTest: BaseCompilationTest, testAction: ScenarioAction ->
                                baseTest.jsScenario(executionStrategyConfiguration.payload, testAction)
                            }
                        } else null)
                }
        }
    }
}


class BtaVersionsCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<KotlinToolchains>> {
            return buildList {
                val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)

                @Suppress("DEPRECATION_ERROR") val kotlinToolchainV1Adapter =
                    if (KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) < KotlinToolingVersion(2, 4, 0, null)) {
                        val asKotlinToolchainsMethod =
                            btaClassloader.loadClass("org.jetbrains.kotlin.buildtools.internal.compat.KotlinToolchainsV1AdapterKt")
                                .getDeclaredMethod("asKotlinToolchains", CompilationService::class.java)
                        asKotlinToolchainsMethod.invoke(
                            null, CompilationService.loadImplementation(
                                btaClassloader
                            )
                        ) as KotlinToolchains
                    } else null
                if (kotlinToolchainV1Adapter != null) {
                    add(
                        named("[v1][${kotlinToolchainV1Adapter.getCompilerVersion()}]", kotlinToolchainV1Adapter)
                    )
                }
                if (kotlinToolchainV1Adapter == null || kotlinToolchainV1Adapter::class != kotlinToolchains::class) {
                    add(
                        named("[v2][${kotlinToolchains.getCompilerVersion()}]", kotlinToolchains)
                    )
                }
            }
        }
    }
}

typealias ProjectCreator = BaseCompilationTest.(ProjectAction) -> Unit
typealias ProjectAction = AbstractProject<out BaseCompilationOperation, out BaseCompilationOperation.Builder, out BaseIncrementalCompilationConfiguration.Builder>.() -> Unit

typealias ScenarioCreator = BaseCompilationTest.(ScenarioAction) -> Unit
typealias ScenarioAction = Scenario<out BaseCompilationOperation.Builder, out BaseIncrementalCompilationConfiguration.Builder>.() -> Unit

fun KotlinToolchains.supportsJs() = KotlinToolingVersion(getCompilerVersion()) >= KotlinToolingVersion(2, 4, 20, null)
