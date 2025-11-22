/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrder
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrderRelation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class CompilerPluginsCustomArgumentConfigurationTest {
    @Test
    @Disabled("Not passing currently")
    fun testDefaultValue() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        assertEquals(emptyList<CompilerPlugin>(), operation.compilerArguments[COMPILER_PLUGINS])
    }

    /**
     * We have no way to check the plugin ordering by a smoke test with the default compiler plugins as they do not have intersecting logic.
     * So, we test this just by ensuring the proper arguments are configured for the compiler.
     */
    @Test
    fun testPluginOrdering() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                emptyList(),
                emptyList(),
                setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.AFTER, "plugin2"))
            ),
            CompilerPlugin(
                "plugin2",
                emptyList(),
                emptyList(),
                setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.BEFORE, "plugin3"))
            ),
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        assertEquals(listOf("-Xcompiler-plugin-order=plugin2>plugin1", "-Xcompiler-plugin-order=plugin2<plugin3"), stringArgumentsDump)
    }

    @Test
    fun testNoPluginOrdering() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                emptyList(),
                emptyList(),
                emptySet(),
            ),
            CompilerPlugin(
                "plugin2",
                emptyList(),
                emptyList(),
                emptySet(),
            ),
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        assertEquals(emptyList<String>(), stringArgumentsDump)
    }

    @Test
    fun testRawArgumentsMarkerPluginDefault() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments.applyArgumentStrings(listOf("-Xplugin=1.jar"))
        assertEquals(
            listOf("___RAW_PLUGINS_APPLIED___"),
            operation.compilerArguments[COMPILER_PLUGINS].map { it.pluginId }
        )
    }

    @Test
    fun testRawArgumentsMarkerPluginModern() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments.applyArgumentStrings(listOf("-Xcompiler-plugin=1.jar"))
        assertEquals(
            listOf("___RAW_PLUGINS_APPLIED___"),
            operation.compilerArguments[COMPILER_PLUGINS].map { it.pluginId }
        )
    }

    @Test
    fun testNoRawArgumentsMarkerPlugin() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments.applyArgumentStrings(listOf())
        assertEquals(
            emptyList<CompilerPlugin>(),
            operation.compilerArguments[COMPILER_PLUGINS]
        )
    }
}