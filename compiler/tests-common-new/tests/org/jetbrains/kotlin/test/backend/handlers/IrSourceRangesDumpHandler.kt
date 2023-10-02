/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.util.RenderIrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.Printer
import java.io.File

class IrSourceRangesDumpHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput>,
) : AbstractIrHandler(testServices, artifactKind) {
    companion object {
        const val DUMP_EXTENSION = "txt"
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives, FirDiagnosticsDirectives)

    private val baseDumper = MultiModuleInfoDumper()
    private val buildersForSeparateFileDumps: MutableMap<File, StringBuilder> = mutableMapOf()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (CodegenTestDirectives.DUMP_SOURCE_RANGES_IR !in module.directives) return
        info.processAllIrModuleFragments(module) { irModuleFragment, moduleName ->
            val builder = baseDumper.builderForModule(moduleName)

            for (irFile in irModuleFragment.files) {
                builder.append(irFile.dumpWithSourceLocations(irFile.fileEntry))
            }
        }
    }

    private fun IrElement.dumpWithSourceLocations(fileEntry: IrFileEntry): String =
        StringBuilder().also {
            acceptVoid(DumpSourceLocations(it, fileEntry))
        }.toString()

    private class DumpSourceLocations(
        out: Appendable,
        val fileEntry: IrFileEntry
    ) : IrElementVisitorVoid {
        val printer = Printer(out, "  ")
        val elementRenderer = RenderIrElementVisitor()

        private fun printElement(element: IrElement) {
            var sourceRangeInfo = fileEntry.getSourceRangeInfo(element.startOffset, element.endOffset)
            if (element.startOffset < 0) {
                sourceRangeInfo = sourceRangeInfo.copy(startLineNumber = -1, startColumnNumber = -1)
            }
            if (element.endOffset < 0) {
                sourceRangeInfo = sourceRangeInfo.copy(endLineNumber = -1, endColumnNumber = -1)
            }
            printer.println("@${sourceRangeInfo.render()} ${element.accept(elementRenderer, null)}")
        }

        override fun visitElement(element: IrElement) {
            printElement(element)
            printer.pushIndent()
            if (element is IrAnnotationContainer && element.annotations.isNotEmpty()) {
                printer.println("annotations:")
                printer.pushIndent()
                for (annotation in element.annotations) {
                    printElement(annotation)
                    printer.pushIndent()
                    annotation.acceptChildrenVoid(this)
                    printer.popIndent()
                }
                printer.popIndent()
            }
            element.acceptChildrenVoid(this)
            printer.popIndent()
        }

        private fun SourceRangeInfo.render() =
            if (startLineNumber == endLineNumber)
                "$startLineNumber:$startColumnNumber..$endColumnNumber"
            else
                "$startLineNumber:$startColumnNumber..$endLineNumber:$endColumnNumber"
    }


    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val moduleStructure = testServices.moduleStructure
        val defaultExpectedFile = moduleStructure.originalTestDataFiles.first()
            .withExtension(moduleStructure.modules.first().getDumpExtension())
        checkOneExpectedFile(defaultExpectedFile, baseDumper.generateResultingDump())
        buildersForSeparateFileDumps.entries.forEach { (expectedFile, dump) -> checkOneExpectedFile(expectedFile, dump.toString()) }
    }

    private fun checkOneExpectedFile(expectedFile: File, actualDump: String) {
        if (actualDump.isNotEmpty()) {
            assertions.assertEqualsToFile(expectedFile, actualDump)
        }
    }

    private fun TestModule.getDumpExtension(ignoreFirIdentical: Boolean = false): String {
        return IrTextDumpHandler.computeDumpExtension(this, DUMP_EXTENSION, ignoreFirIdentical)
    }
}
