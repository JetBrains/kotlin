/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.openapi.util.io.FileUtil.loadFile
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.ir.inline.DumpSyntheticAccessors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.InTextDirectivesUtils.isDirectiveDefined
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import java.io.File

abstract class SyntheticAccessorsDumpHandler<A : ResultingArtifact.Binary<A>>(
    testServices: TestServices,
    artifactKind: BinaryKind<A>,
) : BinaryArtifactHandler<A>(
    testServices,
    artifactKind,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false
) {
    final override fun processModule(module: TestModule, info: A) = Unit

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val testModules = testServices.moduleStructure.modules

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(testModules.first())
        val dumpDir = DumpSyntheticAccessors.getDumpDirectoryOrNull(configuration) ?: return
        val withNarrowedVisibility = configuration.getBoolean(KlibConfigurationKeys.SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY)

        val uniqueIrModuleNames = testModules.mapNotNull { testModule ->
            testServices.dependencyProvider.getArtifactSafe(testModule, BackendKinds.IrBackend)?.irModuleFragment?.name
        }.toSet()

        assertions.assertSyntheticAccessorDumpIsCorrect(
            dumpDir = dumpDir,
            moduleNames = uniqueIrModuleNames,
            testDataFile = testServices.moduleStructure.originalTestDataFiles.first(),
            withNarrowedVisibility
        )
    }

    companion object {
        fun Assertions.assertSyntheticAccessorDumpIsCorrect(
            dumpDir: File,
            moduleNames: Set<Name>,
            testDataFile: File,
            withNarrowedVisibility: Boolean = false
        ) {
            val irModuleDumps = moduleNames.mapNotNull { moduleName ->
                val moduleDumpFile = DumpSyntheticAccessors.getDumpFileForModule(dumpDir, moduleName)
                if (!moduleDumpFile.isFile) return@mapNotNull null

                moduleName to moduleDumpFile.readText().trimEnd()
            }.toMap()

            val actualDump = if (irModuleDumps.isEmpty()) {
                "/* empty dump */\n"
            } else {
                buildString {
                    irModuleDumps.entries.sortedBy { it.key }.forEach { (_, moduleDump) ->
                        if (isNotEmpty()) appendLine().appendLine()
                        appendLine(moduleDump)
                    }
                }
            }

            val expectedDumpFile = if (withNarrowedVisibility) {
                val normalDumpFile = dumpFile(testDataFile, false)
                val narrowedDumpFile = dumpFile(testDataFile, true)

                checkDumpFilesAndChooseOne(testDataFile, normalDumpFile, narrowedDumpFile)
            } else {
                dumpFile(testDataFile, false)
            }

            assertEqualsToFile(expectedDumpFile, actualDump)
        }

        private fun dumpFile(testDataFile: File, withNarrowedVisibility: Boolean): File {
            val dumpFileName = buildString {
                append(testDataFile.nameWithoutExtension)
                append(".accessors")
                if (withNarrowedVisibility) append("-narrowed")
                append(".txt")
            }

            return testDataFile.resolveSibling(dumpFileName)
        }

        private fun Assertions.checkDumpFilesAndChooseOne(testDataFile: File, normalDumpFile: File, narrowedDumpFile: File): File {
            val shouldBeIdenticalDumps = isDirectiveDefined(loadFile(testDataFile), IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS.name)

            if (normalDumpFile.exists() && narrowedDumpFile.exists()) {
                val identicalDumps = normalDumpFile.readText().trimEnd() == narrowedDumpFile.readText().trimEnd()

                fun fail(problem: String, actions: String): Nothing = fail { "$problem\n$actions\n" }

                if (identicalDumps) {
                    if (shouldBeIdenticalDumps)
                        fail(
                            "The synthetic accessor dumps are identical.",
                            "Please remove the .accessors-narrowed.txt file."
                        )
                    else
                        fail(
                            "The synthetic accessor dumps are identical.",
                            "Please remove the .accessors-narrowed.txt file and add the IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS directive to the test data file."
                        )
                } else if (shouldBeIdenticalDumps) {
                    fail(
                        "The synthetic accessor dumps differ.",
                        "Please remove the IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS directive from the test data file."
                    )
                }
            }

            return if (shouldBeIdenticalDumps) normalDumpFile else narrowedDumpFile
        }
    }
}

class JsSyntheticAccessorsDumpHandler(testServices: TestServices) :
    SyntheticAccessorsDumpHandler<BinaryArtifacts.Js>(testServices, ArtifactKinds.Js)
