/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.backend.ir.DumpSyntheticAccessors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import java.io.File

class SyntheticAccessorsDumpHandler(
    testServices: TestServices,
) : AbstractIrHandler(testServices) {
    var dumpSyntheticAccessors : DumpSyntheticAccessors? = null

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(KlibBasedCompilerTestDirectives)

    override fun processModule(module: TestModule, info: IrBackendInput) {
        require(info is IrBackendInput.DeserializedFromKlib) {
            "SyntheticAccessorsDumpHandler works only with DeserializedFromKlib, but got ${info::class.simpleName}"
        }
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val dumpDir = DumpSyntheticAccessors.getDumpDirectoryOrNull(configuration) ?: return
        if (dumpSyntheticAccessors == null)
            dumpSyntheticAccessors = DumpSyntheticAccessors(dumpDir)

        info.moduleInfo.allDependencies.forEach { dumpSyntheticAccessors!!.lower(it) }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val testModules = testServices.moduleStructure.modules

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(testModules.first())
        val dumpDir = DumpSyntheticAccessors.getDumpDirectoryOrNull(configuration) ?: return

        val uniqueIrModuleNames = testModules.mapNotNull { testModule ->
            testServices.artifactsProvider.getArtifactSafe(testModule, BackendKinds.IrBackend)?.irModuleFragment?.name
        }.toSet()

        assertions.assertSyntheticAccessorDumpIsCorrect(
            dumpDir = dumpDir,
            moduleNames = uniqueIrModuleNames,
            testDataFile = testServices.moduleStructure.originalTestDataFiles.first(),
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

            val expectedDumpFile = dumpFile(testDataFile)

            assertEqualsToFile(expectedDumpFile, actualDump)
        }

        private fun dumpFile(testDataFile: File): File {
            val dumpFileName = testDataFile.nameWithoutExtension + ".accessors.txt"
            return testDataFile.resolveSibling(dumpFileName)
        }
    }
}
