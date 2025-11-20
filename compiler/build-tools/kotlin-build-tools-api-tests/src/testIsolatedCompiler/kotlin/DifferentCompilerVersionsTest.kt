/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths

class DifferentCompilerVersionsTest : BaseCompilationTest() {
    @Test
    @DisplayName("Test that different compiler versions can be used with isolated classloader")
    @TestMetadata("jvm-module-1")
    fun olderCompilerImpl() {
        val compilerClasspath = System.getProperty("kotlin.build-tools-api.test.compilerClasspath").split(File.pathSeparator)
            .map { Paths.get(it).toUri().toURL() }
        val stdlibClasspath = System.getProperty("kotlin.build-tools-api.test.stdlibClasspath").split(File.pathSeparator)
            .map { Paths.get(it) }
        val compilerClassloader = URLClassLoader(compilerClasspath.toTypedArray(), SharedApiClassesClassLoader())
        val toolchain = KotlinToolchains.loadImplementation(compilerClassloader)
        project(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1", stdlibClasspath = stdlibClasspath)
            module1.compile {
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }
        }
    }
}