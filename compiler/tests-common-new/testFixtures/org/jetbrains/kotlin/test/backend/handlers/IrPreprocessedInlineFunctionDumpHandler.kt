/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.preparedInlineFunctionCopies
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import java.lang.StringBuilder

private const val PREPROCESSED_INLINE_FUNCTIONS_EXTENSION = "preprocessed.ir.txt"

class IrPreprocessedInlineFunctionDumpHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput>,
) : AbstractIrHandler(testServices, artifactKind) {
    private val dumper = MultiModuleInfoDumper()

    override fun processModule(
        module: TestModule,
        info: IrBackendInput,
    ) {
        if (DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS !in module.directives) return

        val functionsToDump = info.irModuleFragment.preparedInlineFunctionCopies?.takeIf { it.isNotEmpty() } ?: return
        val builder = dumper.builderForModule(module)
        val dumpOptions = DumpIrTreeOptions(printFilePath = false)

        builder.append("Module: ${module.name}\n")
        functionsToDump.forEach { it.dumpWithCallableId(builder, dumpOptions) }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val expectedFile = testServices.moduleStructure.originalTestDataFiles.first().withExtension(PREPROCESSED_INLINE_FUNCTIONS_EXTENSION)
        assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
    }

    private fun IrSimpleFunction.dumpWithCallableId(stringBuilder: StringBuilder, dumpOptions: DumpIrTreeOptions) {
        stringBuilder.append("// CallableId=$callableId\n")
        stringBuilder.append(dump(dumpOptions))
    }
}
