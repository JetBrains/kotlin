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
import org.jetbrains.kotlin.buildtools.api.abi.dumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.dumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.filters
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.nio.file.Paths

class AbiValidationBuildToolsOptionsTests {
    @Test
    @DisplayName("Check default values of options in DumpJvmAbiToStringOperation")
    fun testDumpJvmDefault() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = toolchain.abiValidation.dumpJvmAbiToStringOperation(StringBuilder(), emptyList())

        assertEquals(null, operation[DumpJvmAbiToStringOperation.PATTERN_FILTERS])
    }

    @Test
    @DisplayName("Check passing option values in DumpJvmAbiToStringOperation")
    fun testDumpJvm() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = toolchain.abiValidation.dumpJvmAbiToStringOperation(StringBuilder(), emptyList()) {
            this[DumpJvmAbiToStringOperation.PATTERN_FILTERS] = filters {
                this[AbiFilters.INCLUDE_NAMED] = setOf("INCLUDE_NAMED")
                this[AbiFilters.INCLUDE_ANNOTATED_WITH] = setOf("INCLUDE_ANNOTATED_WITH")
                this[AbiFilters.EXCLUDE_NAMED] = setOf("EXCLUDE_NAMED")
                this[AbiFilters.EXCLUDE_ANNOTATED_WITH] = setOf("EXCLUDE_ANNOTATED_WITH")
            }
        }
        val filters = operation[DumpJvmAbiToStringOperation.PATTERN_FILTERS]
        assertNotNull(filters)
        assertEquals(setOf("INCLUDE_NAMED"), filters[AbiFilters.INCLUDE_NAMED])
        assertEquals(setOf("INCLUDE_ANNOTATED_WITH"), filters[AbiFilters.INCLUDE_ANNOTATED_WITH])
        assertEquals(setOf("EXCLUDE_NAMED"), filters[AbiFilters.EXCLUDE_NAMED])
        assertEquals(setOf("EXCLUDE_ANNOTATED_WITH"), filters[AbiFilters.EXCLUDE_ANNOTATED_WITH])
    }


    @Test
    @DisplayName("Check that mutating the builder does not affect the already built operation for DumpJvmAbiToStringOperation")
    fun testDumpJvmImmutability() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        val jvmBuilder = toolchain.abiValidation.dumpJvmAbiToStringOperationBuilder(StringBuilder(), emptyList())

        jvmBuilder[DumpJvmAbiToStringOperation.PATTERN_FILTERS] = jvmBuilder.filters {
            this[AbiFilters.INCLUDE_NAMED] = setOf("FIRST")
        }
        val operation1 = jvmBuilder.build()

        jvmBuilder[DumpJvmAbiToStringOperation.PATTERN_FILTERS] = jvmBuilder.filters {
            this[AbiFilters.INCLUDE_NAMED] = setOf("SECOND")
        }
        val operation2 = jvmBuilder.build()


        val filters1 = operation1[DumpJvmAbiToStringOperation.PATTERN_FILTERS]
        assertNotNull(filters1)

        val filters2 = operation2[DumpJvmAbiToStringOperation.PATTERN_FILTERS]
        assertNotNull(filters2)

        assertEquals(setOf("FIRST"), filters1[AbiFilters.INCLUDE_NAMED])
        assertEquals(setOf("SECOND"), filters2[AbiFilters.INCLUDE_NAMED])
    }

    @Test
    @DisplayName("Check default values of options in DumpKlibAbiToStringOperation")
    fun testDumpKlibDefault() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = toolchain.abiValidation.dumpKlibAbiToStringOperation(StringBuilder(), mapOf())

        assertEquals(null, operation[DumpKlibAbiToStringOperation.PATTERN_FILTERS])
        assertEquals(null, operation[DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE])
        assertEquals(emptySet<KlibTargetId>(), operation[DumpKlibAbiToStringOperation.TARGETS_TO_INFER])
    }

    @Test
    @DisplayName("Check passing option values in DumpJvmAbiToStringOperation")
    fun testDumpKlib() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = toolchain.abiValidation.dumpKlibAbiToStringOperation(StringBuilder(), mapOf()) {
            this[DumpKlibAbiToStringOperation.PATTERN_FILTERS] = filters {
                this[AbiFilters.INCLUDE_NAMED] = setOf("INCLUDE_NAMED")
                this[AbiFilters.INCLUDE_ANNOTATED_WITH] = setOf("INCLUDE_ANNOTATED_WITH")
                this[AbiFilters.EXCLUDE_NAMED] = setOf("EXCLUDE_NAMED")
                this[AbiFilters.EXCLUDE_ANNOTATED_WITH] = setOf("EXCLUDE_ANNOTATED_WITH")
            }
            this[DumpKlibAbiToStringOperation.TARGETS_TO_INFER] = setOf(KlibTargetId(KlibTargetType.LINUX_ARM64, "customName"))
            this[DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE] = Paths.get(".")
        }

        val filters = operation[DumpKlibAbiToStringOperation.PATTERN_FILTERS]
        assertNotNull(filters)
        assertEquals(setOf("INCLUDE_NAMED"), filters[AbiFilters.INCLUDE_NAMED])
        assertEquals(setOf("INCLUDE_ANNOTATED_WITH"), filters[AbiFilters.INCLUDE_ANNOTATED_WITH])
        assertEquals(setOf("EXCLUDE_NAMED"), filters[AbiFilters.EXCLUDE_NAMED])
        assertEquals(setOf("EXCLUDE_ANNOTATED_WITH"), filters[AbiFilters.EXCLUDE_ANNOTATED_WITH])

        assertEquals(
            setOf(KlibTargetId(KlibTargetType.LINUX_ARM64, "customName")),
            operation[DumpKlibAbiToStringOperation.TARGETS_TO_INFER]
        )
        assertEquals(Paths.get("."), operation[DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE])
    }

    @Test
    @DisplayName("Check that mutating the builder does not affect the already built operation for DumpKlibAbiToStringOperation")
    fun testDumpKlibImmutability() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        val builder = toolchain.abiValidation.dumpKlibAbiToStringOperationBuilder(StringBuilder(), mapOf())

        builder[DumpKlibAbiToStringOperation.PATTERN_FILTERS] = builder.filters {
            this[AbiFilters.INCLUDE_NAMED] = setOf("FIRST")
        }
        builder[DumpKlibAbiToStringOperation.TARGETS_TO_INFER] = setOf(KlibTargetId(KlibTargetType.LINUX_ARM64, "first"))
        builder[DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE] = Paths.get("first")
        val operation1 = builder.build()

        builder[DumpKlibAbiToStringOperation.PATTERN_FILTERS] = builder.filters {
            this[AbiFilters.INCLUDE_NAMED] = setOf("SECOND")
        }
        builder[DumpKlibAbiToStringOperation.TARGETS_TO_INFER] = setOf(KlibTargetId(KlibTargetType.LINUX_X64, "second"))
        builder[DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE] = Paths.get("second")
        val operation2 = builder.build()

        val filters1 = operation1[DumpKlibAbiToStringOperation.PATTERN_FILTERS]
        val filters2 = operation2[DumpKlibAbiToStringOperation.PATTERN_FILTERS]

        assertNotNull(filters1)
        assertNotNull(filters2)
        assertEquals(setOf("FIRST"), filters1[AbiFilters.INCLUDE_NAMED])
        assertEquals(setOf("SECOND"), filters2[AbiFilters.INCLUDE_NAMED])

        assertEquals(setOf(KlibTargetId(KlibTargetType.LINUX_ARM64, "first")), operation1[DumpKlibAbiToStringOperation.TARGETS_TO_INFER])
        assertEquals(setOf(KlibTargetId(KlibTargetType.LINUX_X64, "second")), operation2[DumpKlibAbiToStringOperation.TARGETS_TO_INFER])

        assertEquals(Paths.get("first"), operation1[DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE])
        assertEquals(Paths.get("second"), operation2[DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE])
    }

    @Test
    @DisplayName("Check default values of options in AbiFilters")
    fun testFiltersDefault() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)

        val jvmBuilder = toolchain.abiValidation.dumpJvmAbiToStringOperationBuilder(StringBuilder(), emptyList())
        val jvmFilters = jvmBuilder.filters {}
        assertEquals(emptySet<String>(), jvmFilters[AbiFilters.INCLUDE_NAMED])
        assertEquals(emptySet<String>(), jvmFilters[AbiFilters.EXCLUDE_NAMED])
        assertEquals(emptySet<String>(), jvmFilters[AbiFilters.INCLUDE_ANNOTATED_WITH])
        assertEquals(emptySet<String>(), jvmFilters[AbiFilters.EXCLUDE_ANNOTATED_WITH])

        val klibBuilder = toolchain.abiValidation.dumpKlibAbiToStringOperationBuilder(StringBuilder(), mapOf())
        val klibFilters = klibBuilder.filters {}
        assertEquals(emptySet<String>(), klibFilters[AbiFilters.INCLUDE_NAMED])
        assertEquals(emptySet<String>(), klibFilters[AbiFilters.EXCLUDE_NAMED])
        assertEquals(emptySet<String>(), klibFilters[AbiFilters.INCLUDE_ANNOTATED_WITH])
        assertEquals(emptySet<String>(), klibFilters[AbiFilters.EXCLUDE_ANNOTATED_WITH])
    }
}
