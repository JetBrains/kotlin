/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.jvm.KaptConfiguration
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputsContains
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Paths

@OptIn(ExperimentalBuildToolsApi::class)
class KaptSmokeTest : BaseCompilationTest() {

    val exampleApClasspath = System.getProperty("EXAMPLE_ANNOTATION_PROCESSOR").split(File.pathSeparator).map { Paths.get(it) }
    val kaptClasspath = System.getProperty("KAPT_COMPILER_PLUGIN").split(File.pathSeparator).map { Paths.get(it) }

    val toolsJar: File
        get() {
            val javaHome = System.getProperty("java.home")
            return File(javaHome, "lib/tools.jar").takeIf(File::exists)
                ?: File(javaHome, "../lib/tools.jar").takeIf(File::exists)
                ?: error("Can't find 'tools.jar' in $javaHome or $javaHome/..")
        }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of Kapt configuration application")
    @TestMetadata("kapt-project")
    fun testKaptConfiguration(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("kapt-project")
            module.compile(compilationConfigAction = {
                it.compilerArguments[CommonToolArguments.VERBOSE] = true
                val kaptConfig =
                    it.kaptCompilerPluginBuilder(
                        kaptClasspath = kaptClasspath,
                        stubsOutputDir = module.outputDirectory.resolve("generated/stubs"),
                        sourcesOutputDir = module.outputDirectory.resolve("generated/source"),
                        annotationProcessorsClasspath = exampleApClasspath
                    ).apply {
                        this[KaptConfiguration.VERBOSE] = true
                        this[KaptConfiguration.TOOLS_JAR] = toolsJar.toPath()
                    }
                        .withStubsPhase()
                        .withAptPhase().apply {
                            this[KaptConfiguration.AptPhase.PROCESS_INCREMENTALLY] = false
                        }.build()
                it.compilerArguments[COMPILER_PLUGINS] = listOf(kaptConfig.toCompilerPlugin())
            }) {
                assertOutputsContains(
                    "generated/stubs/error/NonExistentClass.java",
                    "generated/stubs/foo/InternalDummy.java",
                    "generated/stubs/foo/TopLevelDummyFunKt.java",
                    "generated/stubs/foo/InternalDummyUser.java",
                    "generated/stubs/example/TestClass.java",
                    "generated/stubs/example/GenError.java",
                    "generated/stubs/example/ExampleAnnotation.java",
                    "generated/stubs/example/ExampleBinaryAnnotation.java",
                    "generated/stubs/example/ExampleRuntimeAnnotation.java",
                    "generated/stubs/example/ExampleSourceAnnotation.java",
                    "generated/stubs/example/KotlinFilerGenerated.java",
                    $$"generated/source/example/GetTestVal$annotationsGenerated.java",
                    "generated/source/example/BinaryAnnotatedTestClassGenerated.java",
                    "generated/source/example/RuntimeAnnotatedTestClassGenerated.java",
                    "generated/source/example/SourceAnnotatedTestClassGenerated.java",
                    "generated/source/example/TestClassGenerated.java",
                    "generated/source/example/TestFunctionGenerated.java",
                    addKotlinModuleFile = false,
                )
            }
        }
    }
}
