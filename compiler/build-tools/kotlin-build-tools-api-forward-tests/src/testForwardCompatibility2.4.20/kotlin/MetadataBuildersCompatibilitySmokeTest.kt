/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.forward.tests

import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain.Companion.metadata
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.supportsMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText

class MetadataBuildersCompatibilitySmokeTest : BaseCompilationTest() {

    @DisplayName("Metadata: toBuilder round-trip does not affect the original operation")
    @DefaultStrategyAgnosticCompilationTest
    fun testMetadataToBuilderOperationImmutability(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        assumeTrue(toolchain.supportsMetadata())
        val sources = listOf(workingDirectory.resolve("common.kt").also { it.writeText("fun f() = 1") })
        val destination = workingDirectory.resolve("klib")
        val original = toolchain.metadata.metadataKlibCompilationOperationBuilder(sources, destination).apply {
            compilerArguments[MetadataArguments.MODULE_NAME] = "original"
        }.build()

        val newBuilder = original.toBuilder()
        newBuilder.compilerArguments[MetadataArguments.MODULE_NAME] = "modified"
        val modified = newBuilder.build()

        assertEquals("original", original.compilerArguments[MetadataArguments.MODULE_NAME])
        assertEquals("modified", modified.compilerArguments[MetadataArguments.MODULE_NAME])
    }
}
