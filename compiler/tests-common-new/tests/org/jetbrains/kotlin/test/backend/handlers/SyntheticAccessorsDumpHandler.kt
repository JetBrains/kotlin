/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.inline.DumpSyntheticAccessors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import java.io.File

abstract class SyntheticAccessorsDumpHandler<A : ResultingArtifact.Binary<A>>(
    testServices: TestServices,
    artifactKind: BinaryKind<A>,
) : BinaryArtifactHandler<A>(
    testServices,
    artifactKind,
) {
    final override fun processModule(module: TestModule, info: A) = Unit

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val testModules = testServices.moduleStructure.modules

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(testModules.first())
        val dumpDir = DumpSyntheticAccessors.getDumpDirectoryOrNull(configuration) ?: return

        val uniqueIrModuleNames = testModules.mapNotNull { testModule ->
            testServices.dependencyProvider.getArtifactSafe(testModule, BackendKinds.IrBackend)?.irModuleFragment?.name
        }.toSet()

        assertions.assertSyntheticAccessorDumpIsCorrect(
            dumpDir = dumpDir,
            moduleNames = uniqueIrModuleNames,
            testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        )
    }

    companion object {
        fun Assertions.assertSyntheticAccessorDumpIsCorrect(
            dumpDir: File,
            moduleNames: Set<Name>,
            testDataFile: File,
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

            val expectedDumpFile = testDataFile.resolveSibling(testDataFile.nameWithoutExtension + ".accessors.txt")

            assertEqualsToFile(expectedDumpFile, actualDump)
        }
    }
}

class JsSyntheticAccessorsDumpHandler(testServices: TestServices) :
    SyntheticAccessorsDumpHandler<BinaryArtifacts.Js>(testServices, ArtifactKinds.Js)
