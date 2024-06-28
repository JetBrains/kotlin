/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.IrVerifier
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler.Companion.groupWithTestFiles
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.EXTERNAL_FILE
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class IrTreeVerifierHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput>,
) : AbstractIrHandler(testServices, artifactKind) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (CodegenTestDirectives.DUMP_IR !in module.directives) return

        val irFiles = info.irModuleFragment.files
        val testFileToIrFile = irFiles.groupWithTestFiles(module)
        for ((testFile, irFile) in testFileToIrFile) {
            if (testFile?.directives?.contains(EXTERNAL_FILE) == true) continue

            IrVerifier(assertions, module.frontendKind == FrontendKinds.FIR).verifyWithAssert(irFile)

            val actualDump = irFile.dumpTreesFromLineNumber(lineNumber = 0, DumpIrTreeOptions(normalizeNames = true))

            val irFileCopy = irFile.deepCopyWithSymbols<IrFile>()
            val dumpOfCopy = irFileCopy.dumpTreesFromLineNumber(lineNumber = 0, DumpIrTreeOptions(normalizeNames = true))
            assertions.assertEquals(actualDump, dumpOfCopy) { "IR dump mismatch after deep copy with symbols" }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
