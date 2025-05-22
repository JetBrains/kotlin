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
    val irModuleDumps = mutableMapOf<String, String>()

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(KlibBasedCompilerTestDirectives)

    override fun processModule(module: TestModule, info: IrBackendInput) {
        require(info is IrBackendInput.DeserializedFromKlib) {
            "SyntheticAccessorsDumpHandler works only with DeserializedFromKlib, but got ${info::class.simpleName}"
        }
        irModuleDumps[info.irModuleFragment.name.asString()] = DumpSyntheticAccessors().dump(info.irModuleFragment)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val testModules = testServices.moduleStructure.modules

        val uniqueIrModuleNames = testModules.mapNotNull { testModule ->
            testServices.artifactsProvider.getArtifactSafe(testModule, BackendKinds.IrBackend)?.irModuleFragment?.name
        }.toSet()

        assertions.assertSyntheticAccessorDumpIsCorrect(
            moduleDumps = irModuleDumps,
            moduleNames = uniqueIrModuleNames,
            testDataFile = testServices.moduleStructure.originalTestDataFiles.first(),
        )
    }

    companion object {
        fun Assertions.assertSyntheticAccessorDumpIsCorrect(
            moduleDumps: Map<String, String>,
            moduleNames: Set<Name>,
            testDataFile: File,
        ) {
            val irModuleDumps = moduleNames.mapNotNull { moduleName: Name ->
                val moduleDump = moduleDumps[moduleName.asString()]
                if (moduleDump != null && moduleDump.isNotEmpty())
                    moduleName to moduleDump.trimEnd()
                else null
            }.toMap()

            val actualDump = if (irModuleDumps.values.all { it.isEmpty() }) {
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
