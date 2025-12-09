/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrder
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrderRelation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

class CompilerPluginsCustomArgumentConfigurationTest {
    @Test
    fun testDefaultValue() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        assertEquals(emptyList<CompilerPlugin>(), operation.compilerArguments[COMPILER_PLUGINS])
    }

    @Test
    fun testBeforeAfterSemanticEquivalence() {
        // Scenario 1: pluginA BEFORE pluginB
        val argsBefore = run {
            val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
            operation.compilerArguments[COMPILER_PLUGINS] = listOf(
                CompilerPlugin(
                    "pluginA",
                    listOf(Paths.get("/pluginA/pluginA.jar")),
                    emptyList(),
                    setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.BEFORE, "pluginB"))
                ),
                CompilerPlugin(
                    "pluginB",
                    listOf(Paths.get("/pluginB/pluginB.jar")),
                    emptyList(),
                    emptySet()
                ),
            )
            operation.compilerArguments.toArgumentStrings()
        }

        // Scenario 2: pluginB AFTER pluginA
        val argsAfter = run {
            val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
            operation.compilerArguments[COMPILER_PLUGINS] = listOf(
                CompilerPlugin(
                    "pluginB",
                    listOf(Paths.get("/pluginB/pluginB.jar")),
                    emptyList(),
                    setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.AFTER, "pluginA"))
                ),
                CompilerPlugin(
                    "pluginA",
                    listOf(Paths.get("/pluginA/pluginA.jar")),
                    emptyList(),
                    emptySet()
                ),
            )
            operation.compilerArguments.toArgumentStrings()
        }

        fun onlyOrderFlags(args: List<String>): List<String> = args.filter { it.startsWith("-Xcompiler-plugin-order=") }
        // Compiler accepts only '>' (left runs before right). Both scenarios must be exactly the same flag
        assertEquals(onlyOrderFlags(argsBefore), onlyOrderFlags(argsAfter))
    }

    @Test
    fun testPluginWithOneOption() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("/test-plugin/test-plugin.jar")),
                listOf(CompilerPluginOption("option1", "value1")),
                emptySet()
            )
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings().filterNot { it.startsWith("-Xplugin=") }
        assertEquals(listOf("-P", "plugin:test-plugin:option1=value1"), stringArgumentsDump)
    }

    @Test
    fun testPluginWithoutAnyOptions() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("/test-plugin/test-plugin.jar")),
                emptyList(),
                emptySet()
            )
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        assertTrue(
            !stringArgumentsDump.contains("-P"),
            "Expected no -P flag (no options provided): $stringArgumentsDump"
        )
    }

    @Test
    fun testPluginWithTwoDifferentOptions() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("/test-plugin/test-plugin.jar")),
                listOf(CompilerPluginOption("option1", "value1"), CompilerPluginOption("option2", "value2")),
                emptySet()
            )
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings().filterNot { it.startsWith("-Xplugin=") }
        assertEquals(listOf("-P", "plugin:test-plugin:option1=value1,plugin:test-plugin:option2=value2"), stringArgumentsDump)
    }

    @Test
    fun testPluginWithOneOptionDifferentValues() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("/test-plugin/test-plugin.jar")),
                listOf(CompilerPluginOption("option1", "value1"), CompilerPluginOption("option1", "value2")),
                emptySet()
            )
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings().filterNot { it.startsWith("-Xplugin=") }
        assertEquals(listOf("-P", "plugin:test-plugin:option1=value1,plugin:test-plugin:option1=value2"), stringArgumentsDump)
    }

    @Test
    @Disabled("KT-83023")
    fun testDuplicateOptionSameKeyValue() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("/test-plugin/test-plugin.jar")),
                listOf(
                    CompilerPluginOption("option1", "value1"),
                    CompilerPluginOption("option1", "value1")
                ),
                emptySet()
            )
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings().filterNot { it.startsWith("-Xplugin=") }
        assertEquals(
            listOf("-P", "plugin:test-plugin:option1=value1,plugin:test-plugin:option1=value1"),
            stringArgumentsDump
        )
    }

    @Test
    @Disabled("KT-83023")
    fun testPluginWithOptionNoValue() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("test-plugin.jar")),
                listOf(CompilerPluginOption("option1", "")),
                emptySet()
            )
        )

        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings().filterNot { it.startsWith("-Xplugin=") }
        assertEquals(listOf("-P", "plugin:test-plugin:option1="), stringArgumentsDump)
    }

    @Test
    @Disabled("KT-83023")
    fun testPluginWithOptionEmptyKey() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("test-plugin.jar")),
                listOf(CompilerPluginOption("", "")),
                emptySet()
            )
        )

        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        println(stringArgumentsDump)
    }

    @Test
    @Disabled("KT-83023")
    fun testPluginWithOptionEmptyKeyAndValue() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("test-plugin.jar")),
                listOf(CompilerPluginOption("", "")),
                emptySet()
            )
        )

        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        assertTrue(
            !stringArgumentsDump.contains("-P"),
            "Expected no -P flag when key and value are empty: $stringArgumentsDump"
        )
    }

    @Test
    @Disabled("KT-83023")
    fun testPluginOptionKeyWithEquals() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("test-plugin.jar")),
                listOf(CompilerPluginOption("key=with=equals", "value")),
                emptySet()
            )
        )

        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        println(stringArgumentsDump)
    }

    @Test
    @Disabled("KT-83023")
    fun testPluginOptionValueWithColon() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("test-plugin.jar")),
                listOf(CompilerPluginOption("key", "value:with:colon")),
                emptySet()
            )
        )

        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        println(stringArgumentsDump)
    }

    @Test
    @Disabled("KT-83023")
    fun testPluginOptionValueWithComma() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "test-plugin",
                listOf(Paths.get("test-plugin.jar")),
                listOf(CompilerPluginOption("key", "value,with,comma")),
                emptySet()
            )
        )

        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        println(stringArgumentsDump)
    }

    /**
     * We have no way to check the plugin ordering by a smoke test with the default compiler plugins as they do not have intersecting logic.
     * So, we test this just by ensuring the proper arguments are configured for the compiler.
     */

    @Test
    fun testSimpleDirectOrder() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "pluginA",
                listOf(Paths.get("pluginA.jar")),
                emptyList(),
                emptySet()
            ),
            CompilerPlugin(
                "pluginB",
                listOf(Paths.get("pluginB.jar")),
                emptyList(),
                setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.AFTER, "pluginA"))
            ),
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings().filter { it.startsWith("-Xcompiler-plugin-order=") }
        assertEquals(listOf("-Xcompiler-plugin-order=pluginA>pluginB"), stringArgumentsDump)
    }

    @Test
    fun testSeveralOrderingToSamePlugin() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                listOf(Paths.get("plugin1.jar")),
                emptyList(),
                setOf(
                    CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.BEFORE, "plugin2"),
                    CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.BEFORE, "plugin3")
                ).toSet()
            ),
            CompilerPlugin(
                "plugin2",
                listOf(Paths.get("plugin2.jar")),
                emptyList(),
                emptySet()
            ),
            CompilerPlugin(
                "plugin3",
                listOf(Paths.get("plugin3.jar")),
                emptyList(),
                emptySet()
            ),
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings().filter { it.startsWith("-Xcompiler-plugin-order=") }
        // Order of two flags is not semantically important; compare as a set
        assertEquals(
            setOf(
                "-Xcompiler-plugin-order=plugin1>plugin2",
                "-Xcompiler-plugin-order=plugin1>plugin3"
            ),
            stringArgumentsDump.toSet()
        )
    }

    @Test
    fun testLinearChainOrdering() {
        // A -> B -> C should produce two flags: A>B and B>C
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "pluginA",
                listOf(Paths.get("pluginA.jar")),
                emptyList(),
                setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.BEFORE, "pluginB"))
            ),
            CompilerPlugin(
                "pluginB",
                listOf(Paths.get("pluginB.jar")),
                emptyList(),
                setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.BEFORE, "pluginC"))
            ),
            CompilerPlugin(
                "pluginC",
                listOf(Paths.get("pluginC.jar")),
                emptyList(),
                emptySet()
            ),
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings().filter { it.startsWith("-Xcompiler-plugin-order=") }
        assertEquals(
            setOf(
                "-Xcompiler-plugin-order=pluginA>pluginB",
                "-Xcompiler-plugin-order=pluginB>pluginC",
            ),
            stringArgumentsDump.toSet()
        )
    }

    @Test
    fun testNoPluginOrdering() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                listOf(Paths.get("plugin1.jar")),
                emptyList(),
                emptySet(),
            ),
            CompilerPlugin(
                "plugin2",
                listOf(Paths.get("plugin2.jar")),
                emptyList(),
                emptySet(),
            ),
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        val orderingFlags = stringArgumentsDump.filter { it.startsWith("-Xcompiler-plugin-order=") }
        assertTrue(orderingFlags.isEmpty(), "Expected no -Xcompiler-plugin-order= flags: $orderingFlags")
    }

    @Test
    fun testPluginOrderingWithEmptyOtherPluginId() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                listOf(Paths.get("plugin1.jar")),
                emptyList(),
                emptySet(),
            ),
            CompilerPlugin(
                "plugin2",
                listOf(Paths.get("plugin2.jar")),
                emptyList(),
                setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.AFTER, ""))
            ),
        )
        val exception = assertThrows<IllegalStateException> {
            operation.compilerArguments.toArgumentStrings()
        }
        assertEquals(
            "Invalid compiler plugin configuration: plugin id is empty in the ordering requirements for plugin 'plugin2'.",
            exception.message
        )
    }

    @Test
    fun testRedundantOrdering() {
        // Redundant consistent requirements: A BEFORE B + B AFTER A should produce A>B only once
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin.one",
                listOf(Paths.get("plugin.one.jar")),
                emptyList(),
                setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.BEFORE, "plugin.two"))
            ),
            CompilerPlugin(
                "plugin.two",
                listOf(Paths.get("plugin.two.jar")),
                emptyList(),
                setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.AFTER, "plugin.one"))
            ),
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        val orderingFlags = stringArgumentsDump.filter { it.startsWith("-Xcompiler-plugin-order=") }
        assertEquals(listOf("-Xcompiler-plugin-order=plugin.one>plugin.two"), orderingFlags, "Expected A>B to appear only once")
    }

    @Test
    @Disabled("KT-83023")
    fun testOrderingToNonExistentPlugin() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                listOf(Paths.get("plugin1.jar")),
                emptyList(),
                setOf(CompilerPluginPartialOrder(CompilerPluginPartialOrderRelation.AFTER, "nonExistentPlugin"))
            )
        )
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings().filter { it.startsWith("-Xcompiler-plugin-order=") }
        // BTA emits the ordering constraint even though the target plugin doesn't exist in the list
        assertEquals(listOf("-Xcompiler-plugin-order=nonExistentPlugin>plugin1"), stringArgumentsDump)
    }

    @Test
    fun testPluginIdEmpty() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "",
                listOf(Paths.get("plugin1.jar")),
                listOf(CompilerPluginOption("arg1", "hello")),
                emptySet()
            )
        )
        val exception = assertThrows<IllegalStateException> {
            operation.compilerArguments.toArgumentStrings()
        }
        assertEquals(
            "Invalid compiler plugin configuration: plugin id is empty.",
            exception.message
        )
    }

    @Test
    @Disabled("KT-83023")
    fun testPluginIdWithSpecialCharacters() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plug:in=bad",
                listOf(Paths.get("bad.plugin.jar")),
                listOf(CompilerPluginOption("key", "value")),
                emptySet()
            )
        )
        val exception = assertThrows<IllegalStateException> {
            operation.compilerArguments.toArgumentStrings()
        }
        assertEquals(
            "Invalid compiler plugin configuration: plugin id 'plug:in=bad' contains forbidden character(s): ':='.",
            exception.message
        )
    }

    @Test
    fun testSingleClasspathEntry() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                listOf(Paths.get("/plugin1/plugin1.jar")),
                emptyList(),
                emptySet()
            )
        )
        val args = operation.compilerArguments.toArgumentStrings()

        assertTrue(args.any { it.endsWith("plugin1.jar") }, "plugin1.jar not found in classpath: $args")
    }

    @Test
    fun testMultipleClasspathEntries() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                listOf(Paths.get("/plugin1/plugin1.jar"), Paths.get("/plugin1/lib.jar")),
                emptyList(),
                emptySet()
            )
        )
        val args = operation.compilerArguments.toArgumentStrings()

        assertTrue(args.any { it.contains("plugin1.jar") }, "plugin1.jar not found in classpath: $args")
        assertTrue(args.any { it.contains("lib.jar") }, "lib.jar not found in classpath: $args")
    }

    @Test
    fun testEmptyClasspath() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                listOf(),
                emptyList(),
                emptySet()
            )
        )
        val exception = assertThrows<IllegalStateException> {
            operation.compilerArguments.toArgumentStrings()
        }
        assertEquals(
            "Invalid compiler plugin configuration: plugin 'plugin1' has empty classpath.",
            exception.message
        )
    }

    @Test
    fun testDuplicateClasspathEntries() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "plugin1",
                listOf(Paths.get("/plugin1.jar"), Paths.get("/plugin1.jar")),
                emptyList(),
                emptySet()
            )
        )
        val args = operation.compilerArguments.toArgumentStrings()

        // Find the -Xplugin argument and count occurrences of plugin1.jar within it
        val xpluginArg = args.find { it.startsWith("-Xplugin=") } ?: ""
        val plugin1JarOccurrences = xpluginArg.split(",").count { it.contains("plugin1.jar") }
        // there's no harm in duplicating it
        assertEquals(2, plugin1JarOccurrences, "Expected plugin1.jar to appear twice in -Xplugin classpath: $args")
    }

    @Test
    @Disabled("KT-83023")
    fun testDuplicatePluginIdsAndClasspathEntries() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "same-plugin-id",
                listOf(Paths.get("/same-plugin/same-plugin-id.jar")),
                listOf(CompilerPluginOption("option1", "value1")),
                emptySet()
            ),
            CompilerPlugin(
                "same-plugin-id",
                listOf(Paths.get("/same-plugin/same-plugin-id.jar"), Paths.get("/same-plugin/lib.jar")),
                listOf(CompilerPluginOption("option2", "value2")),
                emptySet()
            )
        )

        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        println(stringArgumentsDump)
        // [-Xplugin=C:\same-plugin\same-plugin-id.jar,C:\same-plugin\same-plugin-id.jar,C:\same-plugin\lib.jar, -P, plugin:same-plugin-id:option1=value1,plugin:same-plugin-id:option2=value2]
    }

    @Test
    fun testDifferentPluginIdsSameClasspath() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        operation.compilerArguments[COMPILER_PLUGINS] = listOf(
            CompilerPlugin(
                "fake-plugin-id-1",
                listOf(Paths.get("/good-lib.jar")),
                listOf(CompilerPluginOption("opt1", "val1")),
                emptySet()
            ),
            CompilerPlugin(
                "fake-plugin-id-2",
                listOf(Paths.get("/good-lib.jar")),
                listOf(CompilerPluginOption("opt2", "val2")),
                emptySet()
            )
        )
        val args = operation.compilerArguments.toArgumentStrings()
        println(args)

        // Find the -Xplugin argument and count occurrences of same-plugin.jar within it
        val xpluginArg = args.find { it.startsWith("-Xplugin=") } ?: ""
        val samePluginJarOccurrences = xpluginArg.split(",").count { it.contains("good-lib.jar") }
        // there's no harm in duplicating it
        assertEquals(2, samePluginJarOccurrences, "Expected same-plugin.jar to appear twice in -Xplugin classpath: $args")

        // Verify both plugin options are present
        val pArg = args.find { it.startsWith("plugin:") } ?: ""
        assertTrue(pArg.contains("plugin:fake-plugin-id-1:opt1=val1"), "Expected fake-plugin-id-1 option in -P: $args")
        assertTrue(pArg.contains("plugin:fake-plugin-id-2:opt2=val2"), "Expected fake-plugin-id-2 option in -P: $args")
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