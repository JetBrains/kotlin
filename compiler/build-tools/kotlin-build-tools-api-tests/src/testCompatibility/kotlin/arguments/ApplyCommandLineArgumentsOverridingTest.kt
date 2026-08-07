/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.jsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.jsLinkingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.jvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain.Companion.wasm
import org.jetbrains.kotlin.buildtools.api.wasm.wasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.wasm.wasmLinkingOperation
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain.Companion.metadata
import org.jetbrains.kotlin.buildtools.api.metadata.metadataKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.io.File
import kotlin.io.path.Path

class ApplyCommandLineArgumentsOverridingTest : BaseCompilationTest() {

    val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)

    private fun runTestForAllOperations(block: CommonCompilerArguments.Builder.() -> Unit) {
        kotlinToolchains.jvm.jvmCompilationOperation(emptyList(), Path("")) {
            compilerArguments.block()
        }
        // other platforms supported since 2.4.20
        if (KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) >= KotlinToolingVersion("2.4.20-snapshot")) {
            kotlinToolchains.js.jsKlibCompilationOperation(emptyList(), Path("")) {
                compilerArguments.block()
            }
            kotlinToolchains.js.jsLinkingOperation(Path(""), Path("")) {
                compilerArguments.block()
            }
            kotlinToolchains.wasm.wasmKlibCompilationOperation(emptyList(), Path("")) {
                compilerArguments.block()
            }
            kotlinToolchains.wasm.wasmLinkingOperation(Path(""), Path("")) {
                compilerArguments.block()
            }
            kotlinToolchains.metadata.metadataKlibCompilationOperation(emptyList(), Path("")) {
                compilerArguments.block()
            }
        }
    }

    @Test
    @DisplayName("test applyCommandLineArguments does not override existing values in new versions")
    fun testDoesNotOverrideValues() {
        runTestForAllOperations {
            // BTA versions before 2.3.20 did not have defaults in compiler arguments
            if (KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) >= KotlinToolingVersion("2.3.20")) {
                assertEquals(false, this[CommonToolArguments.VERBOSE])
            }

            this[CommonCompilerArguments.LANGUAGE_VERSION] = KotlinVersion.V1_7
            this[CommonToolArguments.VERBOSE] = true

            applyCommandLineArguments(listOf("-Werror"))
            assertEquals(KotlinVersion.V1_7, this[CommonCompilerArguments.LANGUAGE_VERSION])
            assertEquals(true, this[CommonToolArguments.VERBOSE])

            assertEquals(true, this[CommonToolArguments.WERROR])
        }
    }

    @Test
    @DisplayName("test applyCommandLineArguments does not override compiler plugins configured by the type-safe plugins configuration")
    fun testUnrelatedArgumentDoesNotOverrideCompilerPlugins() {
        // COMPILER_PLUGINS didn't exist before 2.3.20
        assumeTrue { KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) >= KotlinToolingVersion("2.3.20") }
        runTestForAllOperations {
            val plugin = org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin(
                "my-plugin",
                listOf(File("some.jar").toPath()),
                listOf(CompilerPluginOption("key", "value")),
                emptySet()
            )
            this[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(plugin)

            applyCommandLineArguments(listOf("-verbose"))

            val plugins = this[CommonCompilerArguments.COMPILER_PLUGINS]
            assertEquals(listOf("my-plugin"), plugins.map { it.pluginId }) { "Compiler version: " + kotlinToolchains.getCompilerVersion() }
            val pattern = """some\.jar""".toRegex()
            assertEquals(1, pattern.findAll(build().toArgumentStrings().single { it.startsWith("-Xplugin=") }).count()) {
                build().toArgumentStrings().joinToString()
            }
        }
    }

    @Test
    @DisplayName("test applyCommandLineArguments updates compiler plugins when COMPILER_PLUGINS empty and related arguments are provided")
    fun testRelatedArgumentDoesOverrideEmptyCompilerPlugins() {
        // COMPILER_PLUGINS didn't exist before 2.3.20
        assumeTrue { KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) >= KotlinToolingVersion("2.3.20") }
        runTestForAllOperations {
            this[CommonCompilerArguments.COMPILER_PLUGINS] = emptyList()

            applyCommandLineArguments(listOf("-Xplugin=some-path.jar"))

            val plugins = this[CommonCompilerArguments.COMPILER_PLUGINS]
            assertEquals(listOf("___RAW_PLUGINS_APPLIED___"), plugins.map { it.pluginId })
        }
    }

    @Test
    @DisplayName("test applyCommandLineArguments updates compiler plugins when COMPILER_PLUGINS provided and related arguments are provided")
    fun testRelatedArgumentDoesOverrideCompilerPlugins() {
        // COMPILER_PLUGINS didn't exist before 2.3.20
        assumeTrue { KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) >= KotlinToolingVersion("2.3.20") }
        runTestForAllOperations {
            val plugin = org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin(
                "my-plugin",
                listOf(File("some.jar").toPath()),
                listOf(CompilerPluginOption("key", "value")),
                emptySet()
            )
            this[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(plugin)
            applyCommandLineArguments(listOf("-Xplugin=some-path.jar", "-P", "plugin:a:b=5"))

            val plugins = this[CommonCompilerArguments.COMPILER_PLUGINS]
            assertEquals(listOf("___RAW_PLUGINS_APPLIED___"), plugins.map { it.pluginId })
            with(build().toArgumentStrings()) {
                if (isVersionThatReplacesArrayArguments()) {
                    assertTrue(contains("-Xplugin=some-path.jar"))
                    assertFalse(any { "some.jar" in it })
                    assertNotNull(singleOrNull { it == "-P" })
                    assertNotNull(singleOrNull { it == "plugin:a:b=5" })
                } else {
                    // before 2.4.0 Array arguments are appended to previous values rather than replacing them
                    assertTrue(contains("-Xplugin=${File("some.jar").absolutePath},some-path.jar"))
                    assertNotNull(singleOrNull { it == "-P" })
                    assertNotNull(singleOrNull { it == "plugin:my-plugin:key=value,plugin:a:b=5" })
                }
            }
        }
    }

    @Test
    @DisplayName("tests the behavior differences between compiler versions in how string parsing replaces or appends to existing arguments")
    fun testOverrideBehaviorOfStringParsing() {
        kotlinToolchains.jvm.jvmCompilationOperation(emptyList(), Path("")) {
            with(compilerArguments) {
                applyCommandLineArguments(listOf("-Xplugin=some-path.jar", "-P", "plugin:a:b=5"))
                with(build().toArgumentStrings()) {
                    assertTrue(contains("-Xplugin=some-path.jar"))
                    assertNotNull(singleOrNull { it == "-P" })
                    assertNotNull(singleOrNull { it == "plugin:a:b=5" })
                }

                applyCommandLineArguments(
                    listOf(
                        "-Xplugin=other-path.jar",
                        "-Xplugin=other-path2.jar",
                        "-P",
                        "plugin:d:e=5",
                        "-P",
                        "plugin:c:d=10"
                    )
                )
                with(build().toArgumentStrings()) {
                    if (isVersionThatReplacesArrayArguments()) {
                        assertTrue(contains("-Xplugin=other-path.jar,other-path2.jar")) 
                        assertFalse(any { "some-path.jar" in it })
                        assertFalse(any { "plugin:a:b=5" in it })
                        assertNotNull(singleOrNull { it == "-P" })
                        assertNotNull(singleOrNull { it == "plugin:d:e=5,plugin:c:d=10" })
                    } else {
                        // before 2.4.0 Array arguments are appended to previous values rather than replacing them
                        assertTrue(contains("-Xplugin=some-path.jar,other-path.jar,other-path2.jar")) 
                        assertNotNull(singleOrNull { it == "-P" })
                        assertNotNull(singleOrNull { it == "plugin:a:b=5,plugin:d:e=5,plugin:c:d=10" })
                    }
                }
            }
        }
    }
}

private fun ApplyCommandLineArgumentsOverridingTest.isVersionThatReplacesArrayArguments(): Boolean =
    KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) >= KotlinToolingVersion("2.4.0")
