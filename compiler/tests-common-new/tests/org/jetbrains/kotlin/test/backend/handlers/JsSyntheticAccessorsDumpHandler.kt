/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.inline.DumpSyntheticAccessors
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

class JsSyntheticAccessorsDumpHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) = Unit

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val testModules = testServices.moduleStructure.modules

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(testModules.first())
        val dumpDir = DumpSyntheticAccessors.getDumpDirectoryOrNull(configuration) ?: return

        val uniqueIrModuleNames = testModules.mapNotNull { testModule ->
            testServices.dependencyProvider.getArtifactSafe(testModule, BackendKinds.IrBackend)?.irModuleFragment?.name
        }.toSet()

        val irModuleDumps = uniqueIrModuleNames.mapNotNull { moduleName ->
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

        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedDumpFile = testDataFile.resolveSibling(testDataFile.nameWithoutExtension + ".accessors.txt")

        assertions.assertEqualsToFile(expectedDumpFile, actualDump)
    }
}