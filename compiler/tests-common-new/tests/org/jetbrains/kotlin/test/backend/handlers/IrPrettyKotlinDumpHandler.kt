/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.util.FakeOverridesStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler.Companion.computeDumpExtension
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler.Companion.groupWithTestFiles
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.EXTERNAL_FILE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_KT_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension

class IrPrettyKotlinDumpHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    companion object {
        const val DUMP_EXTENSION = "kt.txt"
    }

    private val dumper = MultiModuleInfoDumper("// MODULE: %s")

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives, FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (DUMP_KT_IR !in module.directives || SKIP_KT_DUMP in module.directives) return

        info.processAllIrModuleFragments(module) { irModuleFragment, moduleName ->
            val irFiles = irModuleFragment.files
            val builder = dumper.builderForModule(moduleName)
            val filteredIrFiles = irFiles.groupWithTestFiles(module).filterNot {
                it.first?.directives?.contains(EXTERNAL_FILE) == true
            }.map { it.second }
            val printFileName = filteredIrFiles.size > 1 || testServices.moduleStructure.modules.size > 1
            for (irFile in filteredIrFiles) {
                val dump = irFile.dumpKotlinLike(
                    KotlinLikeDumpOptions(
                        printFileName = printFileName,
                        printFilePath = false,
                        printFakeOverridesStrategy = FakeOverridesStrategy.NONE
                    )
                )
                builder.append(dump)
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val moduleStructure = testServices.moduleStructure
        val extension = computeDumpExtension(moduleStructure.modules.first(), DUMP_EXTENSION)
        val expectedFile = moduleStructure.originalTestDataFiles.first().withExtension(extension)
        assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
    }
}
