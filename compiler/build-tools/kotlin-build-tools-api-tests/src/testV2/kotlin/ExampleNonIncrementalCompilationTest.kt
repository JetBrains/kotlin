/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.JvmModule
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.toPath
import kotlin.io.path.walk

class ExampleNonIncrementalCompilationTest : BaseCompilationTest() {
    @DisplayName("Sample non-incremental compilation test with two modules")
    @DefaultStrategyAgnosticCompilationTest
    fun myTest(strategyConfig: CompilerExecutionStrategyConfiguration) = runTest {
        val kt = KotlinToolchain.loadImplementation(this.javaClass.classLoader)


        project(strategyConfig) {
            val module1 = module("jvm-module-1")
            val compilationOperation = kt.jvm.createJvmCompilationOperation(
                module1.sourcesDirectory.walk()
                    .filter { path -> path.pathString.run { setOf("kt", "kts").any { endsWith(".$it") } } }
                    .toList(), module1.outputDirectory.toAbsolutePath()
            )

            val stdlibLocation: Path =
                KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath()

            compilationOperation.compilerArguments.apply {
                set(JvmCompilerArguments.NO_REFLECT, true)
                set(JvmCompilerArguments.NO_STDLIB, true)
                set(
                    JvmCompilerArguments.CLASSPATH,
                    (module1 as JvmModule).dependencies.map { it.location }.plusElement(stdlibLocation).joinToString(File.pathSeparator)
                )
                set(JvmCompilerArguments.MODULE_NAME, module1.moduleName)
            }
            println(kt.executeOperation(compilationOperation))
            assertOutputs(module1, setOf("FooKt.class", "Bar.class", "BazKt.class"))
        }
    }
}
