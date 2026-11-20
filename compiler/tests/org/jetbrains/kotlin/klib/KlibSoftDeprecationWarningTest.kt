/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.backend.common.warnAboutSoftDeprecatedAbiVersions
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_PROVIDER
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.collections.set
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

@OptIn(MessageCollectorAccess::class)
class KlibSoftDeprecationWarningTest {
    @TempDir
    private lateinit var tmpDir: Path

    @Test
    fun testSoftDeprecationWarning() {
        val libraries = listOf(
            generateNewKlib(
                uniqueName = "ok1",
                abiVersion = KotlinAbiVersion.CURRENT,
                compilerVersion = null,
            ),
            generateNewKlib(
                uniqueName = "ok2",
                abiVersion = KotlinAbiVersion.CURRENT,
                compilerVersion = KotlinAbiVersion.CURRENT.toCompilerVersion(),
            ),
            generateNewKlib(
                uniqueName = "ok3",
                abiVersion = KotlinAbiVersion.FIRST_SUPPORTED_WITHOUT_DEPRECATION,
                compilerVersion = null,
            ),
            generateNewKlib(
                uniqueName = "ok4",
                abiVersion = KotlinAbiVersion.FIRST_SUPPORTED_WITHOUT_DEPRECATION,
                compilerVersion = KotlinAbiVersion.FIRST_SUPPORTED_WITHOUT_DEPRECATION.toCompilerVersion(),
            ),
            generateNewKlib(
                uniqueName = "deprecated1",
                abiVersion = KotlinAbiVersion.FIRST_SUPPORTED_WITHOUT_DEPRECATION.prev(),
                compilerVersion = null,
            ),
            generateNewKlib(
                uniqueName = "deprecated2",
                abiVersion = KotlinAbiVersion.FIRST_SUPPORTED_WITHOUT_DEPRECATION.prev(),
                compilerVersion = KotlinAbiVersion.FIRST_SUPPORTED_WITHOUT_DEPRECATION.prev().toCompilerVersion(),
            ),
            generateNewKlib(
                uniqueName = "deprecated3",
                abiVersion = KotlinAbiVersion.FIRST_SUPPORTED_WITHOUT_DEPRECATION.prev().prev(),
                compilerVersion = null,
            ),
            generateNewKlib(
                uniqueName = "deprecated4",
                abiVersion = KotlinAbiVersion.FIRST_SUPPORTED_WITHOUT_DEPRECATION.prev().prev(),
                compilerVersion = KotlinAbiVersion.FIRST_SUPPORTED_WITHOUT_DEPRECATION.prev().prev().toCompilerVersion(),
            ),
        )

        val messageCollector = MessageCollectorImpl()
        val compilerConfiguration = CompilerConfiguration.create().apply {
            this.messageCollector = messageCollector
        }

        val loadingResult = KlibLoader {
            libraryPaths(libraries.map { it.pathString })
        }.load()
            .warnAboutSoftDeprecatedAbiVersions(compilerConfiguration)

        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(compilerConfiguration.diagnosticsCollector, compilerConfiguration)

        assertFalse(loadingResult.hasProblems)
        assertEquals(libraries.size, loadingResult.librariesStdlibFirst.size)

        assertEquals(1, messageCollector.messages.size)

        val actualMessage = messageCollector.messages.single()
        assertEquals(CompilerMessageSeverity.STRONG_WARNING, actualMessage.severity)
        assertNull(actualMessage.location)

        val actualMessageTextFiltered = actualMessage.message.lineSequence()
            .map { it.replace(tmpDir.pathString, "<path_prefix>") }
            .map { it.replace('\\', '/') }
            .joinToString(separator = "\n")

        assertEquals(
            """
                Legacy ABI version warning.

                There are libraries with ABI versions that will no longer be supported by future compiler versions:
                - <path_prefix>/deprecated1, ABI version 1.7.0
                - <path_prefix>/deprecated2, ABI version 1.7.0 (produced by compiler 1.7.0)
                - <path_prefix>/deprecated3, ABI version 1.6.0
                - <path_prefix>/deprecated4, ABI version 1.6.0 (produced by compiler 1.6.0)
                
                We recommend updating your project settings to use newer library versions compatible with Kotlin compiler 1.9.20 or later (ABI version 1.8.0 or later).
            """.trimIndent(),
            actualMessageTextFiltered.trim(),
        )
    }

    private fun KotlinAbiVersion.toCompilerVersion(): String = toString()

    private fun KotlinAbiVersion.prev(): KotlinAbiVersion =
        if (minor == 0) KotlinAbiVersion(major - 1, 255, 0) else KotlinAbiVersion(major, minor - 1, 0)

    private fun generateNewKlib(uniqueName: String, abiVersion: KotlinAbiVersion, compilerVersion: String?): Path {
        val klibDir = tmpDir.apply { createDirectories() }.resolve(uniqueName)

        assertFalse(klibDir.exists()) { "KLIB should not exist before compilation: $klibDir" }

        // Write a fake library with the required unique name.
        KlibWriter {
            manifest {
                moduleName(uniqueName)
                versions(
                    KotlinLibraryVersioning(
                        compilerVersion = compilerVersion,
                        abiVersion = abiVersion,
                        metadataVersion = null,
                    )
                )
                platformAndTargets(BuiltInsPlatform.NATIVE) // Does not matter.
                manifest {
                    customProperties {
                        // Emulate the presence of ABI.
                        this[KLIB_PROPERTY_IR_PROVIDER] = KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
                    }
                }
            }
        }.writeTo(klibDir)

        assertTrue(klibDir.isDirectory()) { "KLIB should exist after compilation: $klibDir" }

        return klibDir
    }
}
