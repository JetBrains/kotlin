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
import java.nio.file.Paths

class CompilerPluginsCustomArgumentConfigurationTest {
    @Test
    fun testDefaultValue() {
        val toolchain = KotlinToolchains.loadImplementation(CompilerPluginsCustomArgumentConfigurationTest::class.java.classLoader)
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
        assertEquals(emptyList<CompilerPlugin>(), operation.compilerArguments[COMPILER_PLUGINS])
    }

    @Test
    @Disabled("Until BTA maps BEFORE to canonical '>' form; compiler parses only '>'. The logic must be fixed on BTA side")
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
    @Disabled("Ask the compiler team about validation on their side")
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
    @Disabled("Ask the compiler team about such case (e.g. should we add '=' ?")
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
    @Disabled("Ask the compiler team about validation on their side")
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
    @Disabled("We shouldn't give to the compiler -P plugin:test-plugin:= if the key and value of compiler option are empty strings. Fix from BTA side is needed")
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
    @Disabled("Ask the compiler team if they have validation for special CLI characters (=, :, ,) in plugin options. BTA should also validate/reject such characters")
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
    @Disabled("Ask the compiler team if they have validation for special CLI characters (=, :, ,) in plugin options. BTA should also validate/reject such characters")
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
    @Disabled("Ask the compiler team if they have validation for special CLI characters (=, :, ,) in plugin options. BTA should also validate/reject such characters")
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
    @Disabled("Until BTA maps BEFORE to canonical '>' form; compiler parses only '>'. The logic must be fixed on BTA side")
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
    @Disabled("Empty otherPluginId in ordering constraints should be validated on BTA side to prevent generating invalid ordering flags like '>plugin2'")
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
        val stringArgumentsDump = operation.compilerArguments.toArgumentStrings()
        val orderingFlags = stringArgumentsDump.filter { it.startsWith("-Xcompiler-plugin-order=") }
        assertTrue(orderingFlags.isEmpty(), "Expected no -Xcompiler-plugin-order= flags: $orderingFlags")
    }

    @Test
    @Disabled("Until BTA maps BEFORE to canonical '>' form; compiler parses only '>'. The logic must be fixed on BTA side")
    // there are two problems: with BEFORE producing < and duplicating the ordering
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
    @Disabled("Ask the compiler team about such case")
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
    @Disabled("Plugin id validation is not defined on BTA side. It should not be allowed to add empty pluginId, especially if we have CompilerPluginOption")
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
        // The current API does not validate pluginId
        val args = operation.compilerArguments.toArgumentStrings()
        println(args) // [-Xplugin=...plugin1.jar, -P, plugin::arg1=hello]
    }

    @Test
    @Disabled("Ask the compiler team if they have validation for special CLI characters (=, :, ,) in plugin options. BTA should also validate/reject such characters")
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
        val args = operation.compilerArguments.toArgumentStrings()
        println(args) // [-Xplugin=.../bad.plugin.jar, -P, plugin:plug:in=bad:key=value]
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
    @Disabled("Empty classpath is not allowed. Fix from BTA side is needed")
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
        val args = operation.compilerArguments.toArgumentStrings()
        println(args) // []
    }

    @Test
    @Disabled("Fix on BTA side is needed: duplicate classpath entries should be merged into one")
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
        assertEquals(1, plugin1JarOccurrences, "Expected plugin1.jar to appear only once in -Xplugin classpath: $args")
    }

    @Test
    @Disabled("Ask the compiler team about such case first")
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