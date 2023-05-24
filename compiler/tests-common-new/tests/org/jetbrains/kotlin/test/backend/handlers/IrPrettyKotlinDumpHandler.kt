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

/**
 * Uses [dumpKotlinLike] to compare the human-readable representation of an IR tree with
 * an expected output in a `*.kt.txt` file located next to the test file.
 *
 * This handler can be enabled by specifying the [DUMP_KT_IR] test directive,
 * or disabled with the [SKIP_KT_DUMP] directive.
 */
class IrPrettyKotlinDumpHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    companion object {
        const val DUMP_EXTENSION = "kt.txt"
    }

    private val dumper = MultiModuleInfoDumper("// MODULE: %s")

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives, FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (DUMP_KT_IR !in module.directives || SKIP_KT_DUMP in module.directives) return
        dumpModuleKotlinLike(
            module, testServices.moduleStructure.modules, info, dumper,
            KotlinLikeDumpOptions(
                printFilePath = false,
                printFakeOverridesStrategy = FakeOverridesStrategy.NONE,
            ),
        )
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val moduleStructure = testServices.moduleStructure
        val extension = computeDumpExtension(moduleStructure.modules.first(), DUMP_EXTENSION)
        val expectedFile = moduleStructure.originalTestDataFiles.first().withExtension(extension)
        assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
    }
}

internal fun dumpModuleKotlinLike(
    module: TestModule,
    allModules: List<TestModule>,
    info: IrBackendInput,
    multiModuleInfoDumper: MultiModuleInfoDumper,
    options: KotlinLikeDumpOptions,
) {
    info.processAllIrModuleFragments(module) { irModuleFragment, moduleName ->
        val irFiles = irModuleFragment.files
        val builder = multiModuleInfoDumper.builderForModule(moduleName)
        val filteredIrFiles = irFiles.groupWithTestFiles(module).filterNot { (testFile, _) ->
            testFile?.let { EXTERNAL_FILE in it.directives || it.isAdditional } ?: false
        }.map { it.second }
        val printFileName = filteredIrFiles.size > 1 || allModules.size > 1
        val modifiedOptions = options.copy(printFileName = printFileName)
        for (irFile in filteredIrFiles) {
            builder.append(irFile.dumpKotlinLike(modifiedOptions))
        }
    }
}
