/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain.Companion.abiValidation
import org.jetbrains.kotlin.buildtools.api.abi.dumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.filters
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk


class AbiValidationBuildToolsJvmTests : BaseCompilationTest() {

    @Test
    @DisplayName("Smoke test of ABI validation for JVM")
    @TestMetadata("jvm-module-1")
    fun testJvmDump() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        jvmProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module = module("jvm-module-1")
            module.compile()

            val classFiles = module.outputDirectory.walk()
                .filter { it.isRegularFile() && it.exists() && it.name.endsWith(".class") }
                .asIterable()

            val dumpAppender = StringBuilder()
            val operation = kotlinToolchain.abiValidation.dumpJvmAbiToStringOperation(dumpAppender, classFiles)
            kotlinToolchain.createBuildSession().use { it.executeOperation(operation) }

            val expectedDump = """
                public final class Bar {
                	public fun <init> ()V
                	public final fun bar ()V
                }

                public final class BazKt {
                	public static final fun baz ()I
                }

                public final class FooKt {
                	public static final fun foo ()V
                }


            """.trimIndent()

            assertEquals(expectedDump, dumpAppender.toString())
        }
    }

    @Test
    @DisplayName("Smoke test of ABI validation filters: every filter form in one run")
    @TestMetadata("jvm-module-abi-filters")
    fun testJvmDumpAllFilters() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        jvmProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module = module("jvm-module-abi-filters")
            module.compile()

            val classFiles = module.outputDirectory.walk()
                .filter { it.isRegularFile() && it.exists() && it.name.endsWith(".class") }
                .asIterable()

            val dump = StringBuilder()
            val operation = kotlinToolchain.abiValidation.dumpJvmAbiToStringOperation(dump, classFiles) {
                this[DumpJvmAbiToStringOperation.PATTERN_FILTERS] = filters {
                    this[AbiFilters.INCLUDE_NAMED] = setOf(
                        "org.example.api.**",
                        "org.example.PublicEntryPoint",
                        "org.example.util.**",
                    )
                    this[AbiFilters.EXCLUDE_NAMED] = setOf(
                        "org.example.impl.**",
                        "org.example.*Generated*",
                        "org.example.util.Tmp?Helper",
                        "org.example.api.*Api",
                    )
                    this[AbiFilters.INCLUDE_ANNOTATED_WITH] = setOf("org.example.api.PublicApi")
                    this[AbiFilters.EXCLUDE_ANNOTATED_WITH] = setOf("org.example.api.InternalApi")
                }
            }
            kotlinToolchain.createBuildSession().use { it.executeOperation(operation) }

            val expectedDump = """
                public final class org/example/PublicEntryPoint {
                	public fun <init> ()V
                	public final fun greet (Ljava/lang/String;)Ljava/lang/String;
                }

                public final class org/example/api/PublicService {
                	public fun <init> ()V
                	public final fun compute (I)I
                }

                public final class org/example/config/BuildConfig {
                	public fun <init> ()V
                	public final fun getBuiltAt ()J
                	public final fun getCommitSha ()Ljava/lang/String;
                }

                public final class org/example/util/TmpABHelper {
                	public fun <init> ()V
                	public final fun work ()V
                }


            """.trimIndent()

            assertEquals(expectedDump, dump.toString())
        }
    }
}
