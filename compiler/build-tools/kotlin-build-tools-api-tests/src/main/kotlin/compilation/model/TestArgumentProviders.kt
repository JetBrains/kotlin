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
import org.jetbrains.kotlin.buildtools.tests.compilation.util.isKotlinNativeCompilerAvailable
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.toKotlinVersion
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.support.ParameterDeclarations
import java.net.URL
import java.util.*
import java.util.stream.Stream

class BtaV2StrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return kotlinToolchains(v2only = true).flatMap { kotlinToolchainsProvider: Named<() -> KotlinToolchains> ->
            executionPolicies().map { executionPolicyProvider: Named<(KotlinToolchains) -> ExecutionPolicy> ->
                val kotlinToolchainArg = named(
                    combineNames(kotlinToolchainsProvider.name, executionPolicyProvider.name),
                    kotlinToolchainsProvider.payload()
                )
                val executionPolicyArg = executionPolicyProvider.payload(kotlinToolchainArg.payload)
                Arguments.of(kotlinToolchainArg, executionPolicyArg)
            }
        }.stream()
    }
}

class BtaV2StrategyAndPlatformAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return kotlinToolchains(v2only = true).flatMap { kotlinToolchainsProvider: Named<() -> KotlinToolchains> ->
            executionPolicies().flatMap { executionPolicyProvider: Named<(KotlinToolchains) -> ExecutionPolicy> ->
                kotlinToolchainsProvider.payload().let { temporaryKotlinToolchains ->
                    temporaryKotlinToolchains.platformProjects()
                        .map { platformProject: Named<(KotlinToolchains, ExecutionPolicy) -> ProjectCreator> ->
                            val kotlinToolchainArg = kotlinToolchainsProvider.payload()
                            val executionPolicyArg = executionPolicyProvider.payload(kotlinToolchainArg)
                            val projectArg = named(
                                combineNames(kotlinToolchainsProvider.name, executionPolicyProvider.name, platformProject.name),
                                platformProject.payload(kotlinToolchainArg, executionPolicyArg)
                            )
                            Arguments.of(projectArg, kotlinToolchainArg, executionPolicyArg)
                        }
                }
            }
        }.stream()
    }
}

class DefaultStrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return kotlinToolchains().flatMap { kotlinToolchainsProvider: Named<() -> KotlinToolchains> ->
            executionPolicies().map { executionPolicyProvider: Named<(KotlinToolchains) -> ExecutionPolicy> ->
                val kotlinToolchainsArg =
                    named(combineNames(kotlinToolchainsProvider.name, executionPolicyProvider.name), kotlinToolchainsProvider.payload())
                val executionPolicyArg = executionPolicyProvider.payload(kotlinToolchainsArg.payload)
                Arguments.of(kotlinToolchainsArg, executionPolicyArg)
            }
        }.stream()
    }
}

/**
 * Fans out over every platform BTAv2 supports without fixing an
 * execution policy. Each argument is a [ProjectWithPolicyCreator] plus the [KotlinToolchains]; the test chooses the policy
 * itself (e.g. in-process) via the toolchain and passes it to the opener.
 */
class BtaV2PlatformAgnosticCompilationTestArgumentProvider : ArgumentsProvider {

    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return kotlinToolchains(v2only = true).flatMap { kotlinToolchainsProvider: Named<() -> KotlinToolchains> ->
            kotlinToolchainsProvider.payload().let { temporaryKotlinToolchains ->
                temporaryKotlinToolchains.platformProjects()
                    .map { platformProject: Named<(KotlinToolchains, ExecutionPolicy) -> ProjectCreator> ->
                        val kotlinToolchainsArg = kotlinToolchainsProvider.payload()
                        val projectArg = named(
                            combineNames(kotlinToolchainsProvider.name, platformProject.name)
                        ) { baseTest: BaseCompilationTest, policy: ExecutionPolicy, action: ProjectAction ->
                            platformProject.payload(kotlinToolchainsArg, policy)(baseTest, action)
                        }
                        Arguments.of(projectArg, kotlinToolchainsArg)
                    }
            }
        }.stream()
    }
}

class DefaultStrategyAndPlatformAgnosticProjectTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<Arguments> =
        kotlinToolchains().flatMap { kotlinToolchainsProvider: Named<() -> KotlinToolchains> ->
            executionPolicies().flatMap { executionPolicyProvider: Named<(KotlinToolchains) -> ExecutionPolicy> ->
                val temporaryKotlinToolchains = kotlinToolchainsProvider.payload()
                temporaryKotlinToolchains.platformProjects()
                    .map { projectProvider: Named<(KotlinToolchains, ExecutionPolicy) -> ProjectCreator> ->
                        val kotlinToolchainsArg = kotlinToolchainsProvider.payload()
                        val executionPolicyArg = executionPolicyProvider.payload(kotlinToolchainsArg)
                        val projectArg = named(
                            combineNames(kotlinToolchainsProvider.name, executionPolicyProvider.name, projectProvider.name),
                            projectProvider.payload(kotlinToolchainsArg, executionPolicyArg)
                        )
                        Arguments.of(projectArg, kotlinToolchainsArg, executionPolicyArg)
                    }
            }
        }.stream()
}

class DefaultStrategyAndPlatformAgnosticScenarioTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<Arguments> =
        kotlinToolchains().flatMap { kotlinToolchainsProvider: Named<() -> KotlinToolchains> ->
            executionPolicies().flatMap { executionPolicyProvider: Named<(KotlinToolchains) -> ExecutionPolicy> ->
                val temporaryKotlinToolchains = kotlinToolchainsProvider.payload()
                temporaryKotlinToolchains.platformScenarios()
                    .map { scenarioProvider: Named<(KotlinToolchains, ExecutionPolicy) -> ScenarioCreator> ->
                        val kotlinToolchainsArg = kotlinToolchainsProvider.payload()
                        val executionPolicyArg = executionPolicyProvider.payload(kotlinToolchainsArg)
                        val scenarioArg = named(
                            combineNames(kotlinToolchainsProvider.name, executionPolicyProvider.name, scenarioProvider.name),
                            scenarioProvider.payload(kotlinToolchainsArg, executionPolicyArg)
                        )
                        Arguments.of(scenarioArg, kotlinToolchainsArg, executionPolicyArg)
                    }
            }
        }.stream()
}

class BtaVersionsCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return kotlinToolchains().map { kotlinToolchainsProvider: Named<() -> KotlinToolchains> ->
            val argument1 = named(combineNames(kotlinToolchainsProvider.name), kotlinToolchainsProvider.payload())
            Arguments.of(argument1)
        }.stream()
    }
}

private fun ClassLoader.withoutV2Implementation(): ClassLoader = object : ClassLoader(this) {
    override fun getResources(name: String): Enumeration<URL> =
        if (name == "META-INF/services/${KotlinToolchains::class.java.name}") Collections.emptyEnumeration() else super.getResources(name)
}

typealias ProjectCreator = BaseCompilationTest.(ProjectAction) -> Unit
typealias ProjectWithPolicyCreator = BaseCompilationTest.(ExecutionPolicy, ProjectAction) -> Unit
typealias ProjectAction = AbstractProject<out BaseCompilationOperation, out BaseCompilationOperation.Builder, out BaseIncrementalCompilationConfiguration.Builder>.() -> Unit

typealias ScenarioCreator = BaseCompilationTest.(ScenarioAction) -> Unit
typealias ScenarioAction = Scenario<out BaseCompilationOperation.Builder, out BaseIncrementalCompilationConfiguration.Builder>.() -> Unit

fun KotlinToolchains.supportsJs() = KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 4, 20)
fun KotlinToolchains.supportsWasm() = KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 4, 20)
fun KotlinToolchains.supportsMetadata() = KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 4, 20)

/**
 * Kotlin/Native is only included when the Kotlin/Native compiler is on the classpath of the implementation under test,
 * see [isKotlinNativeCompilerAvailable].
 */
fun KotlinToolchains.supportsNative() =
    KotlinToolingVersion(getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 5, 20) && isKotlinNativeCompilerAvailable


fun kotlinToolchains(v2only: Boolean = false): List<Named<() -> KotlinToolchains>> {
    return buildList {
        val version = KotlinToolchains.loadImplementation(btaClassloader).getCompilerVersion()
        if (!v2only && KotlinToolingVersion(version) < KotlinToolingVersion(2, 4, 0, null)) {
            add(named("$version][v1") { KotlinToolchains.loadImplementation(btaClassloader.withoutV2Implementation()) })
        }
        if (v2only || KotlinToolingVersion(version) >= KotlinToolingVersion(2, 3, 0, null)) {
            add(named("$version][v2") { KotlinToolchains.loadImplementation(btaClassloader) })
        }
    }
}

fun executionPolicies(): List<Named<(KotlinToolchains) -> ExecutionPolicy>> = buildList {
    add(named("in-process") { kotlinToolchains: KotlinToolchains -> kotlinToolchains.createInProcessExecutionPolicy() })
    add(named("daemon") { kotlinToolchains: KotlinToolchains -> kotlinToolchains.daemonExecutionPolicy { } })
}

fun KotlinToolchains.platformScenarios(): List<Named<(KotlinToolchains, ExecutionPolicy) -> ScenarioCreator>> = buildList {
    add(named("JVM") { kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy ->
        { baseTest: BaseCompilationTest, testAction: ScenarioAction ->
            baseTest.jvmScenario(kotlinToolchains, executionPolicy, testAction)
        }
    })

    if (supportsJs()) {
        add(named("JS") { kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy ->
            { baseTest: BaseCompilationTest, testAction: ScenarioAction ->
                baseTest.jsScenario(kotlinToolchains, executionPolicy, testAction)
            }
        })
    }
    if (supportsWasm()) {
        add(named("Wasm") { kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy ->
            { baseTest: BaseCompilationTest, testAction: ScenarioAction ->
                baseTest.wasmScenario(kotlinToolchains, executionPolicy, testAction)
            }
        })
    }
}

fun KotlinToolchains.platformProjects(): List<Named<(KotlinToolchains, ExecutionPolicy) -> ProjectCreator>> = buildList {
    add(named("JVM") { kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy ->
        { baseTest: BaseCompilationTest, testAction: ProjectAction ->
            baseTest.jvmProject(kotlinToolchains, executionPolicy, testAction)
        }
    })

    if (supportsJs()) {
        add(named("JS") { kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy ->
            { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                baseTest.jsProject(kotlinToolchains, executionPolicy, testAction)
            }
        })
    }
    if (supportsWasm()) {
        add(named("Wasm") { kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy ->
            { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                baseTest.wasmProject(kotlinToolchains, executionPolicy, testAction)
            }
        })
    }
    if (supportsMetadata()) {
        add(named("Metadata") { kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy ->
            { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                baseTest.metadataProject(kotlinToolchains, executionPolicy, testAction)
            }
        })
    }
    if (supportsNative()) {
        add(named("Native") { kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy ->
            { baseTest: BaseCompilationTest, testAction: ProjectAction ->
                baseTest.nativeProject(kotlinToolchains, executionPolicy, testAction)
            }
        })
    }
}

fun combineNames(vararg names: String) = names.joinToString("") { "[$it]" }
