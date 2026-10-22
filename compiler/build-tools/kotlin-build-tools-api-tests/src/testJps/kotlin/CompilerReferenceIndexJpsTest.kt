/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class, ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.FILE_IDS_TO_PATHS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.LOOKUPS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.SUBTYPES_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.cri
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readBytes

@DisplayName("The compiler reference index of a JPS-managed compilation")
class CompilerReferenceIndexJpsTest : BaseJpsTest() {

    @DisplayName("A second compilation into the same directory appends its data")
    @Test
    @TestMetadata(FIXTURE)
    fun secondCompilationAppends() {
        val toolchain = toolchain
        jvmProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val first = module(FIXTURE)
            val second = module(SECOND_FIXTURE)
            val criDirectory = first.buildDirectory.resolve("cri")
            first.compile(compilationConfigAction = { it.withCompilerReferenceIndex(criDirectory) })
            second.compile(compilationConfigAction = { it.withCompilerReferenceIndex(criDirectory) }) {
                val index = toolchain.readIndex(criDirectory)
                assertTrue(first.sourcesDirectory.resolve("derived.kt").toRealPath() in index.filesLookingUp("base"))
                assertTrue(second.sourcesDirectory.resolve("bar.kt").toRealPath() in index.filesLookingUp("foo"))
            }
        }
    }

    @DisplayName("Subtypes are written for an experimental language version, whose metadata is newer than the default")
    @Test
    @TestMetadata(FIXTURE)
    fun subtypesAreWrittenForExperimentalLanguageVersion() {
        val toolchain = toolchain
        jvmProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module = module(FIXTURE)
            val criDirectory = module.buildDirectory.resolve("cri")
            module.compile(compilationConfigAction = {
                it.compilerArguments[LANGUAGE_VERSION] = KotlinVersion.V2_7
                it.withCompilerReferenceIndex(criDirectory)
            }) {
                assertEquals(setOf("Derived"), toolchain.readIndex(criDirectory).subtypesOf("Base"))
            }
        }
    }

    @DisplayName("The JPS trackers still receive everything when the index is collected")
    @Test
    @TestMetadata(FIXTURE)
    fun jpsTrackersStillReceiveEverything() {
        val toolchain = toolchain
        jvmProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module = module(FIXTURE)
            val criDirectory = module.buildDirectory.resolve("cri")
            val lookupTracker = RecordingLookupTracker()
            val fileMappingTracker = RecordingFileMappingTracker()
            module.compile(compilationConfigAction = {
                it.withCompilerReferenceIndex(criDirectory) {
                    this[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER] = lookupTracker
                    this[JvmJpsManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER] = fileMappingTracker
                }
            }) {
                assertTrue(lookupTracker.lookups.any { it.filePath.endsWith("derived.kt") && it.name == "base" }) {
                    "The JPS lookup tracker didn't receive the base() lookup from derived.kt: ${lookupTracker.lookups}"
                }
                assertTrue(fileMappingTracker.sourcesToOutput.any { it.second.endsWith("Derived.class") }) {
                    "The JPS file mapping tracker didn't receive Derived.class: ${fileMappingTracker.sourcesToOutput}"
                }
                val index = toolchain.readIndex(criDirectory)
                assertTrue(module.sourcesDirectory.resolve("derived.kt").toRealPath() in index.filesLookingUp("base")) {
                    "derived.kt is missing from the files looking up base(): ${index.filesLookingUp("base")}"
                }
                assertEquals(setOf("Derived"), index.subtypesOf("Base"))
            }
        }
    }

    @DisplayName("Nothing is written when the index isn't requested")
    @Test
    @TestMetadata(FIXTURE)
    fun nothingIsWrittenWithoutTheFlag() {
        val toolchain = toolchain
        jvmProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module = module(FIXTURE)
            val criDirectory = module.buildDirectory.resolve("cri")
            module.compile(compilationConfigAction = {
                it.withJpsIc { this[JvmJpsManagedIncrementalCompilationConfiguration.COMPILER_REF_INDEX_DIRECTORY] = criDirectory }
            }) {
                assertFalse(criDirectory.exists())
            }
        }
    }

    @DisplayName("Without a directory, nothing is written and a warning is logged")
    @Test
    @TestMetadata(FIXTURE)
    fun warningWithoutDirectory() {
        val toolchain = toolchain
        jvmProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module = module(FIXTURE)
            module.compile(compilationConfigAction = { it.withCompilerReferenceIndex(criDirectory = null) }) {
                assertLogContainsPatterns(
                    LogLevel.WARN,
                    Regex(".*JvmJpsManagedIncrementalCompilationConfiguration\\.COMPILER_REF_INDEX_DIRECTORY isn't set.*"),
                )
                assertFalse(module.buildDirectory.resolve("cri").exists())
            }
        }
    }

    private companion object {
        const val FIXTURE = "jps-compiler-reference-index"
        const val SECOND_FIXTURE = "basic-multimodule-project/module-1"
    }
}

private fun JvmCompilationOperation.Builder.withCompilerReferenceIndex(
    criDirectory: Path?,
    configure: JvmJpsManagedIncrementalCompilationConfiguration.Builder.() -> Unit = {},
) {
    this[BaseCompilationOperation.GENERATE_COMPILER_REF_INDEX] = true
    withJpsIc {
        this[JvmJpsManagedIncrementalCompilationConfiguration.COMPILER_REF_INDEX_DIRECTORY] = criDirectory
        configure()
    }
}

private class CompilerReferenceIndex(
    private val lookups: Map<Int, Set<Int>>,
    private val filePaths: Map<Int, String>,
    private val subtypes: Map<Int, Set<String>>,
) {
    /** Returns the real paths of the files that looked up [fqName], so that symlinked temporary directories compare equal. */
    fun filesLookingUp(fqName: String): Set<Path> =
        lookups[fqName.hashCode()].orEmpty().map { Paths.get(filePaths.getValue(it)).toRealPath() }.toSet()

    fun subtypesOf(fqName: String): Set<String> = subtypes[fqName.hashCode()].orEmpty()
}

private fun KotlinToolchains.readIndex(criDirectory: Path): CompilerReferenceIndex = createBuildSession().use { session ->
    val lookups = session.executeOperation(
        cri.createCriLookupDataDeserializationOperation(criDirectory.resolve(LOOKUPS_FILENAME).readBytes())
    )
    val filePaths = session.executeOperation(
        cri.createCriFileIdToPathDataDeserializationOperation(criDirectory.resolve(FILE_IDS_TO_PATHS_FILENAME).readBytes())
    )
    val subtypes = session.executeOperation(
        cri.createCriSubtypeDataDeserializationOperation(criDirectory.resolve(SUBTYPES_FILENAME).readBytes())
    )
    CompilerReferenceIndex(
        lookups = lookups.groupBy({ it.fqNameHashCode!! }, { it.fileIds }).mapValues { it.value.flatten().toSet() },
        filePaths = filePaths.associate { it.fileId!! to it.path!! },
        subtypes = subtypes.groupBy({ it.fqNameHashCode!! }, { it.subtypes }).mapValues { it.value.flatten().toSet() },
    )
}
