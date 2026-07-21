/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.Scenario
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.toKotlinVersion
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.support.ParameterDeclarations
import java.util.stream.Stream

class BtaV2StrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<Pair<KotlinToolchains, ExecutionPolicy>>> {
            val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
            val v2Args: List<Named<Pair<KotlinToolchains, ExecutionPolicy>>> = listOf(
                named(
                    "[2.4.0][in-process]",
                    kotlinToolchains to kotlinToolchains.createInProcessExecutionPolicy()
                ), named(
                    "[2.4.0][daemon]",
                    kotlinToolchains to kotlinToolchains.daemonExecutionPolicyBuilder().build()
                )
            )

            return v2Args
        }
    }
}

class BtaV2StrategyAndPlatformAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
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
                    },
                )
            }
        }
    }
}

class DefaultStrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
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
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
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
                        },
                    )
                }
        }
    }
}

class DefaultStrategyAndPlatformAgnosticScenarioTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
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
                        },
                    )
                }
        }
    }
}


class BtaVersionsCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
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
                        named("[2.4.0]", kotlinToolchains)
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

fun KotlinToolchains.supportsJs() = KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 4, 20)
fun KotlinToolchains.supportsWasm() = KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 4, 20)
fun KotlinToolchains.supportsMetadata() = KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 4, 20)
