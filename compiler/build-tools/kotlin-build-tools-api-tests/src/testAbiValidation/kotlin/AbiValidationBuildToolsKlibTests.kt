/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain.Companion.abiValidation
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetType
import org.jetbrains.kotlin.buildtools.api.abi.dumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.filters
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jsProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


class AbiValidationBuildToolsKlibTests : BaseCompilationTest() {

    @Test
    @DisplayName("Smoke test of ABI validation for klib")
    @TestMetadata("js-ic-basic-lib")
    fun testJsKlibDump() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        jsProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module = module("js-ic-basic-lib")
            module.compile()

            val dump = StringBuilder()
            val operation = kotlinToolchain.abiValidation.dumpKlibAbiToStringOperation(
                dump,
                mapOf(KlibTargetId(KlibTargetType.JS, "myJs") to module.outputDirectory)
            )
            kotlinToolchain.createBuildSession().use { it.executeOperation(operation) }

            val expectedDump = """
                // Klib ABI Dump
                // Targets: [js.myJs]
                // Rendering settings:
                // - Signature version: 2
                // - Show manifest properties: true
                // - Show declarations: true

                // Library unique name: <js-ic-basic-lib>
                final class /A { // /A|null[0]
                    constructor <init>() // /A.<init>|<init>(){}[0]

                    final val x // /A.x|{}x[0]
                        final fun <get-x>(): kotlin/Int // /A.x.<get-x>|<get-x>(){}[0]
                }

                final class /DummyInLibMain { // /DummyInLibMain|null[0]
                    constructor <init>() // /DummyInLibMain.<init>|<init>(){}[0]
                }

                final fun /useAInLibMain(/A) // /useAInLibMain|useAInLibMain(A){}[0]

            """.trimIndent()

            assertEquals(expectedDump, dump.toString())
        }
    }

    @Test
    @DisplayName("Smoke test of ABI validation filters for klib")
    @TestMetadata("js-ic-basic-lib")
    fun testJsKlibDumpWithFilters() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        jsProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module = module("js-ic-basic-lib")
            module.compile()

            val dump = StringBuilder()
            val operation = kotlinToolchain.abiValidation.dumpKlibAbiToStringOperation(
                dump,
                mapOf(KlibTargetId(KlibTargetType.JS, "js") to module.outputDirectory)
            ) {
                this[DumpKlibAbiToStringOperation.PATTERN_FILTERS] = filters {
                    this[AbiFilters.INCLUDE_NAMED] = setOf("A", "useAInLibMain")
                    this[AbiFilters.EXCLUDE_NAMED] = setOf("DummyInLibMain")
                }
            }
            kotlinToolchain.createBuildSession().use { it.executeOperation(operation) }

            val expectedDump = """
                // Klib ABI Dump
                // Targets: [js]
                // Rendering settings:
                // - Signature version: 2
                // - Show manifest properties: true
                // - Show declarations: true

                // Library unique name: <js-ic-basic-lib>
                final class /A // /A|null[0]

                final fun /useAInLibMain(/A) // /useAInLibMain|useAInLibMain(A){}[0]

            """.trimIndent()

            assertEquals(expectedDump, dump.toString())
        }
    }
}
