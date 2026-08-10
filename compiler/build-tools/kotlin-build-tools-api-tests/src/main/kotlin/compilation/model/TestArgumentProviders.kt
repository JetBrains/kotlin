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
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.wasmScenario
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.testFederation.TestFederationMode
import org.jetbrains.kotlin.testFederation.testFederationMode
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
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().map { Arguments.of(it) }.stream()
    }

    companion object {
        fun namedStrategyArguments(): List<Named<ProjectCreator>> {
            return BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments().flatMap { strategy ->
                val toolchain = strategy.payload.first
                val policy = strategy.payload.second
                BtaV2PlatformAgnosticCompilationTestArgumentProvider.projectWithPolicyCreators(toolchain, strategy.name)
                    .map { platform ->
                        named(platform.name) { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                            platform.payload(baseTest, policy, testAction)
                        }
                    }
            }
        }
    }
}

class DefaultStrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<Arguments> =
        namedStrategyProviders().map { Arguments.of(named(it.name, it.payload())) }.stream()

    companion object {
        fun namedStrategyProviders(): List<Named<() -> Pair<KotlinToolchains, ExecutionPolicy>>> {
            return BtaVersionsCompilationTestArgumentProvider.namedToolchainProviders().flatMap { namedArgument ->
                listOfNotNull(
                    named(
                        "${namedArgument.name}[in-process]"
                    ) {
                        namedArgument.payload().let { it to it.createInProcessExecutionPolicy() }
                    },
                    // We do not test the daemon when running in Smoke test mode
                    if (testFederationMode != TestFederationMode.Smoke) {
                        named(
                            "${namedArgument.name}[daemon]"
                        ) {
                            namedArgument.payload().let { it to it.daemonExecutionPolicy() }
                        }
                    } else null
                )
            }
        }
    }
}

/**
 * Fans out over every platform BTAv2 supports without fixing an
 * execution policy. Each argument is a [ProjectWithPolicyCreator] plus the [KotlinToolchains]; the test chooses the policy
 * itself (e.g. in-process) via the toolchain and passes it to the opener.
 */
class BtaV2PlatformAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        return projectWithPolicyCreators(toolchain, "[v2][${toolchain.getCompilerVersion()}]")
            .map { Arguments.of(it, toolchain) }.stream()
    }

    companion object {
        fun projectWithPolicyCreators(toolchain: KotlinToolchains, labelPrefix: String): List<Named<ProjectWithPolicyCreator>> {
            return listOfNotNull(
                named("$labelPrefix[JVM]") { baseTest: BaseCompilationTest, policy: ExecutionPolicy, action: ProjectAction ->
                    baseTest.jvmProject(toolchain, policy, action)
                },
                if (toolchain.supportsJs()) named("$labelPrefix[JS]") { baseTest: BaseCompilationTest, policy: ExecutionPolicy, action: ProjectAction ->
                    baseTest.jsProject(toolchain, policy, action)
                } else null,
                if (toolchain.supportsWasm()) named("$labelPrefix[Wasm]") { baseTest: BaseCompilationTest, policy: ExecutionPolicy, action: ProjectAction ->
                    baseTest.wasmProject(toolchain, policy, action)
                } else null,
                if (toolchain.supportsMetadata()) named("$labelPrefix[Metadata]") { baseTest: BaseCompilationTest, policy: ExecutionPolicy, action: ProjectAction ->
                    baseTest.metadataProject(toolchain, policy, action)
                } else null,
            )
        }
    }
}

class DefaultStrategyAndPlatformAgnosticProjectTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<Arguments> =
        namedProjectProviders().map { Arguments.of(named(it.name, it.payload())) }.stream()

    companion object {
        fun namedProjectProviders(): List<Named<() -> ProjectCreator>> {
            return DefaultStrategyAgnosticCompilationTestArgumentProvider.namedStrategyProviders()
                .flatMap { executionStrategyConfiguration ->
                    val temporaryKotlinToolchains = executionStrategyConfiguration.payload().first
                    listOfNotNull(
                        named(
                            "${executionStrategyConfiguration.name}[JVM]"
                        ) {
                            { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                                baseTest.jvmProject(executionStrategyConfiguration.payload(), testAction)
                            }
                        },
                        if (temporaryKotlinToolchains.supportsJs()) {
                            named(
                                "${executionStrategyConfiguration.name}[JS]"
                            ) {
                                { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                                    baseTest.jsProject(executionStrategyConfiguration.payload(), testAction)
                                }
                            }
                        } else null,
                        if (temporaryKotlinToolchains.supportsWasm()) {
                            named(
                                "${executionStrategyConfiguration.name}[Wasm]"
                            ) {
                                { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                                    baseTest.wasmProject(executionStrategyConfiguration.payload(), testAction)
                                }
                            }
                        } else null,
                        if (temporaryKotlinToolchains.supportsMetadata()) {
                            named(
                                "${executionStrategyConfiguration.name}[Metadata]"
                            ) {
                                { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                                    baseTest.metadataProject(executionStrategyConfiguration.payload(), testAction)
                                }
                            }
                        } else null,
                    )
                }
        }
    }
}

class DefaultStrategyAndPlatformAgnosticScenarioTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> =
        namedScenarioProviders().map { Arguments.of(named(it.name, it.payload())) }.stream()

    companion object {
        fun namedScenarioProviders(): List<Named<() -> ScenarioCreator>> {
            return DefaultStrategyAgnosticCompilationTestArgumentProvider.namedStrategyProviders()
                .flatMap { executionStrategyConfiguration ->
                    val temporaryKotlinToolchains = executionStrategyConfiguration.payload().first

                    listOfNotNull(
                        named(
                            "${executionStrategyConfiguration.name}[JVM]"
                        ) {
                            { baseTest: BaseCompilationTest, testAction: ScenarioAction ->
                                baseTest.jvmScenario(executionStrategyConfiguration.payload(), testAction)
                            }
                        }, if (temporaryKotlinToolchains.supportsJs()) {
                            named(
                                "${executionStrategyConfiguration.name}[JS]"
                            ) {
                                { baseTest: BaseCompilationTest, testAction: ScenarioAction ->
                                    baseTest.jsScenario(executionStrategyConfiguration.payload(), testAction)
                                }
                            }
                        } else null,
                        if (temporaryKotlinToolchains.supportsWasm()) {
                            named(
                                "${executionStrategyConfiguration.name}[Wasm]"
                            ) {
                                { baseTest: BaseCompilationTest, testAction: ScenarioAction ->
                                    baseTest.wasmScenario(executionStrategyConfiguration.payload(), testAction)
                                }
                            }
                        } else null
                    )
                }
        }
    }
}

class BtaVersionsCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<Arguments> =
        namedToolchainProviders().map { Arguments.of(named(it.name, it.payload())) }.stream()

    companion object {
        fun namedToolchainProviders(): List<Named<() -> KotlinToolchains>> {
            return buildList {
                val kotlinToolchainsProvider = { KotlinToolchains.loadImplementation(btaClassloader) }
                val temporaryKotlinToolchains = kotlinToolchainsProvider()
                val version = temporaryKotlinToolchains.getCompilerVersion()

                @Suppress("DEPRECATION_ERROR") val kotlinToolchainV1Adapter =
                    if (KotlinToolingVersion(version) < KotlinToolingVersion(2, 4, 0, null)) {
                        {
                            val asKotlinToolchainsMethod =
                                btaClassloader.loadClass("org.jetbrains.kotlin.buildtools.internal.compat.KotlinToolchainsV1AdapterKt")
                                    .getDeclaredMethod("asKotlinToolchains", CompilationService::class.java)
                            asKotlinToolchainsMethod.invoke(
                                null, CompilationService.loadImplementation(
                                    btaClassloader
                                )
                            ) as KotlinToolchains
                        }
                    } else null
                if (kotlinToolchainV1Adapter != null) {
                    add(
                        named("[v1][$version]", kotlinToolchainV1Adapter)
                    )
                }
                if (kotlinToolchainV1Adapter == null || kotlinToolchainV1Adapter()::class != temporaryKotlinToolchains::class) {
                    add(
                        named("[v2][$version]", kotlinToolchainsProvider)
                    )
                }
            }
        }
    }
}

typealias ProjectCreator = BaseCompilationTest.(ProjectAction) -> Unit
typealias ProjectWithPolicyCreator = BaseCompilationTest.(ExecutionPolicy, ProjectAction) -> Unit
typealias ProjectAction = AbstractProject<out BaseCompilationOperation, out BaseCompilationOperation.Builder, out BaseIncrementalCompilationConfiguration.Builder>.() -> Unit

typealias ScenarioCreator = BaseCompilationTest.(ScenarioAction) -> Unit
typealias ScenarioAction = Scenario<out BaseCompilationOperation.Builder, out BaseIncrementalCompilationConfiguration.Builder>.() -> Unit

fun KotlinToolchains.supportsJs() = KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 4, 20)
fun KotlinToolchains.supportsWasm() = KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 4, 20)
fun KotlinToolchains.supportsMetadata() = KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 4, 20)
