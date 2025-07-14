/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.backend.ir.DumpSyntheticAccessors
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import java.io.File

class SyntheticAccessorsDumpHandler(
    testServices: TestServices,
) : AbstractIrHandler(testServices) {
    private val dumper = MultiModuleInfoDumper("")

    override fun processModule(module: TestModule, info: IrBackendInput) {
        require(info is IrBackendInput.DeserializedFromKlib) {
            "SyntheticAccessorsDumpHandler works only with DeserializedFromKlib, but got ${info::class.simpleName}"
        }
        val dump = DumpSyntheticAccessors.dump(info.irModuleFragment).removeSuffix("\n")
        dumper.builderForModule(info.irModuleFragment.name.asString()).append(dump)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertions.assertEqualsToFile(
            dumpFile(testServices.moduleStructure.originalTestDataFiles.first()),
            dumper.generateResultingDump().trim().ifEmpty { "/* empty dump */" }
        )
    }

    companion object {
        private fun dumpFile(testDataFile: File): File {
            val dumpFileName = testDataFile.nameWithoutExtension + ".accessors.txt"
            return testDataFile.resolveSibling(dumpFileName)
        }
    }
}
