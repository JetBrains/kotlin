/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.util.*
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
import org.jetbrains.kotlin.utils.Printer
import java.io.File

/**
 * Uses [KotlinLikeDumper] to compare the human-readable representation of an IR tree with
 * an expected output in a `*.kt.txt` file located next to the test file.
 *
 * This handler can be enabled by specifying the [DUMP_KT_IR] test directive,
 * or disabled with the [SKIP_KT_DUMP] directive.
 */
open class IrPrettyKotlinDumpHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    companion object {
        const val DUMP_EXTENSION = "kt.txt"
    }

    private val dumper = MultiModuleInfoDumper("// MODULE: %s")

    protected open fun getDumpExtension(module: TestModule) = computeDumpExtension(module, DUMP_EXTENSION)

    protected val expectedFile: File by lazy(LazyThreadSafetyMode.NONE) {
        val moduleStructure = testServices.moduleStructure
        val extension = getDumpExtension(moduleStructure.modules.first())
        moduleStructure.originalTestDataFiles.first().withExtension(extension)
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives, FirDiagnosticsDirectives)

    protected open fun createDumper(
        module: TestModule,
        info: IrBackendInput,
        builder: StringBuilder,
        printFileName: Boolean
    ) = KotlinLikeDumper(
        Printer(builder, 1, "  "),
        KotlinLikeDumpOptions(
            printFileName = printFileName,
            printFilePath = false,
            printFakeOverridesStrategy = FakeOverridesStrategy.NONE,
        )
    )

    protected fun dumpModule(module: TestModule, info: IrBackendInput) {
        val irFiles = info.irModuleFragment.files
        val builder = dumper.builderForModule(module)
        val filteredIrFiles = irFiles.groupWithTestFiles(module).filterNot {
            it.first?.directives?.contains(EXTERNAL_FILE) == true
        }.map { it.second }
        val printFileName = filteredIrFiles.size > 1 || testServices.moduleStructure.modules.size > 1
        for (irFile in filteredIrFiles) {
            createDumper(module, info, builder, printFileName).printElement(irFile)
        }
    }

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (DUMP_KT_IR !in module.directives || SKIP_KT_DUMP in module.directives) return
        dumpModule(module, info)
    }


    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val dump = dumper.generateResultingDump()
        assertions.assertEqualsToFile(expectedFile, dump)
    }
}
