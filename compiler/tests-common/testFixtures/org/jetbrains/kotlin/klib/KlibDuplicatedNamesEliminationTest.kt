/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.backend.common.eliminateLibrariesWithDuplicatedUniqueNames
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.duplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.test.TestCaseWithTmpdir

// TODO (KT-76785): Handling of duplicated names in KLIBs is a workaround that needs to be removed in the future.
class KlibDuplicatedNamesEliminationTest : TestCaseWithTmpdir() {
    private var generatedLibsCounter = 0

    // A baseline test.
    fun testNoDuplicatedNames() {
        val libraryPaths = listOf(
            generateNewKlib("a"),
            generateNewKlib("b"),
            generateNewKlib("c"),
        )

        val resultOfLoading = KlibLoader { libraryPaths(libraryPaths) }.load()
        assertFalse(resultOfLoading.hasProblems)
        assertEquals(libraryPaths, resultOfLoading.librariesStdlibFirst.map { it.libraryFile.path })

        for (strategy in DuplicatedUniqueNameStrategy.entries) {
            val compilerConfiguration = CompilerConfiguration().apply {
                this.duplicatedUniqueNameStrategy = strategy
                this.messageCollector = object : MessageCollectorImpl() {
                    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                        fail("$severity: $message at $location")
                    }
                }
            }

            val resultOfElimination = resultOfLoading.eliminateLibrariesWithDuplicatedUniqueNames(compilerConfiguration)
            assertTrue(resultOfElimination === resultOfLoading)
        }
    }

    fun testDuplicatedNamesAllowAllWithWarning() {
        val libraryPaths = listOf(
            generateNewKlib("a"),
            generateNewKlib("b"),
            generateNewKlib("b"),
            generateNewKlib("c"),
            generateNewKlib("c"),
        )

        val resultOfLoading = KlibLoader { libraryPaths(libraryPaths) }.load()
        assertFalse(resultOfLoading.hasProblems)
        assertEquals(libraryPaths, resultOfLoading.librariesStdlibFirst.map { it.libraryFile.path })

        val messageCollector = MessageCollectorImpl()
        val compilerConfiguration = CompilerConfiguration().apply {
            this.duplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING
            this.messageCollector = messageCollector
        }

        val resultOfElimination = resultOfLoading.eliminateLibrariesWithDuplicatedUniqueNames(compilerConfiguration)
        assertTrue(resultOfElimination === resultOfLoading)

        assertEquals(2, messageCollector.messages.size)
        assertTrue(messageCollector.messages.all { it.severity == CompilerMessageSeverity.STRONG_WARNING })

        assertEquals(
            "KLIB loader: The same 'unique_name=b' found in more than one library: ${libraryPaths[1]}, ${libraryPaths[2]}",
            messageCollector.messages[0].message
        )

        assertEquals(
            "KLIB loader: The same 'unique_name=c' found in more than one library: ${libraryPaths[3]}, ${libraryPaths[4]}",
            messageCollector.messages[1].message
        )
    }

    fun testDuplicatedNamesAllowFirstWithWarning() {
        val libraryPaths = listOf(
            generateNewKlib("a"),
            generateNewKlib("b"),
            generateNewKlib("b"),
            generateNewKlib("c"),
            generateNewKlib("c"),
        )

        val resultOfLoading = KlibLoader { libraryPaths(libraryPaths) }.load()
        assertFalse(resultOfLoading.hasProblems)
        assertEquals(libraryPaths, resultOfLoading.librariesStdlibFirst.map { it.libraryFile.path })

        val messageCollector = MessageCollectorImpl()
        val compilerConfiguration = CompilerConfiguration().apply {
            this.duplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING
            this.messageCollector = messageCollector
        }

        val resultOfElimination = resultOfLoading.eliminateLibrariesWithDuplicatedUniqueNames(compilerConfiguration)
        assertTrue(resultOfElimination !== resultOfLoading)
        assertFalse(resultOfElimination.hasProblems)
        assertEquals(
            listOf(libraryPaths[0], libraryPaths[1], libraryPaths[3]),
            resultOfElimination.librariesStdlibFirst.map { it.libraryFile.path }
        )

        assertEquals(2, messageCollector.messages.size)
        assertTrue(messageCollector.messages.all { it.severity == CompilerMessageSeverity.STRONG_WARNING })

        assertEquals(
            "KLIB loader: The same 'unique_name=b' found in more than one library: ${libraryPaths[1]}, ${libraryPaths[2]}",
            messageCollector.messages[0].message
        )

        assertEquals(
            "KLIB loader: The same 'unique_name=c' found in more than one library: ${libraryPaths[3]}, ${libraryPaths[4]}",
            messageCollector.messages[1].message
        )
    }

    fun testDuplicatedNamesDeny() {
        val libraryPaths = listOf(
            generateNewKlib("a"),
            generateNewKlib("b"),
            generateNewKlib("b"),
            generateNewKlib("c"),
            generateNewKlib("c"),
        )

        val resultOfLoading = KlibLoader { libraryPaths(libraryPaths) }.load()
        assertFalse(resultOfLoading.hasProblems)
        assertEquals(libraryPaths, resultOfLoading.librariesStdlibFirst.map { it.libraryFile.path })

        val messageCollector = MessageCollectorImpl()
        val compilerConfiguration = CompilerConfiguration().apply {
            this.duplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.DENY
            this.messageCollector = messageCollector
        }

        val resultOfElimination = resultOfLoading.eliminateLibrariesWithDuplicatedUniqueNames(compilerConfiguration)
        assertTrue(resultOfElimination === resultOfLoading)

        assertEquals(2, messageCollector.messages.size)
        assertTrue(messageCollector.messages.all { it.severity == CompilerMessageSeverity.ERROR })

        assertTrue(
            messageCollector.messages[0].message.startsWith(
                "KLIB loader: The same 'unique_name=b' found in more than one library: ${libraryPaths[1]}, ${libraryPaths[2]}\nPlease file an issue to"
            )
        )

        assertTrue(
            messageCollector.messages[1].message.startsWith(
                "KLIB loader: The same 'unique_name=c' found in more than one library: ${libraryPaths[3]}, ${libraryPaths[4]}\nPlease file an issue to"
            )
        )
    }

    private fun generateNewKlib(uniqueName: String): String {
        val libraryBaseDir = tmpdir.resolve("lib${generatedLibsCounter++}").apply { mkdirs() }
        val klibDir = libraryBaseDir.resolve(uniqueName)

        assertFalse("KLIB should not exist before compilation: $klibDir", klibDir.exists())

        // Write a fake library with the required unique name.
        buildKotlinLibrary(
            linkDependencies = emptyList(),
            metadata = SerializedMetadata(byteArrayOf(), emptyList(), emptyList()),
            ir = null,
            versions = KotlinLibraryVersioning(null, null, null, KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS),
            output = klibDir.path,
            moduleName = uniqueName,
            nopack = true,
            manifestProperties = null,
            builtInsPlatform = BuiltInsPlatform.COMMON, // Does not matter.
        )

        assertTrue("KLIB should exist after compilation: $klibDir", klibDir.isDirectory)

        return klibDir.path
    }
}
