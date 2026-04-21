/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain.Companion.abiValidation
import org.jetbrains.kotlin.buildtools.api.abi.dumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
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
    fun test() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        jvmProject(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
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
}
