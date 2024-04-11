/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.createFilesWithGeneratedDeclarations
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.test.backend.handlers.assertFileDoesntExist
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper

class FirVFirDumpHandler(
    testServices: TestServices,
) : FirAnalysisHandler(testServices) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper()
    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            if (FirDiagnosticsDirectives.DUMP_VFIR !in currentModule.directives) return
            val builderForModule = dumper.builderForModule(currentModule)
            val firFiles = info.mainFirFiles

            val allFiles = buildList {
                addAll(firFiles.values)
                addAll(part.session.createFilesWithGeneratedDeclarations())
            }
            part.session.lazyDeclarationResolver.startResolvingPhase(FirResolvePhase.BODY_RESOLVE)

            val renderer = FirVerboseRenderer(FirVerbosePrinter(builderForModule))
            allFiles.forEach { it.accept(renderer) }

            part.session.lazyDeclarationResolver.finishResolvingPhase(FirResolvePhase.BODY_RESOLVE)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.parentFile.resolve("${testDataFile.nameWithoutFirExtension}.vfir.txt")

        if (dumper.isEmpty()) {
            assertions.assertFileDoesntExist(expectedFile, FirDiagnosticsDirectives.DUMP_VFIR)
        } else {
            val actualText = dumper.generateResultingDump()
            assertions.assertEqualsToFile(expectedFile, actualText, message = { "Content is not equal" })
        }
    }

    private class FirVerboseRenderer(private val printer: FirVerbosePrinter) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            printer.print(element.javaClass.simpleName)
            renderInChildrenSection { element.acceptChildren(this) }
        }

        override fun visitBlock(block: FirBlock) {
            printer.print(block.javaClass.simpleName)
            renderInDetailsSection {
                printer.print("source.kind: ${block.source!!.kind.javaClass.simpleName}")
            }
            renderInChildrenSection {
                block.acceptChildren(this)
            }
        }

        private fun renderInDetailsSection(render: () -> Unit) {
            printer.print("{")
            printer.printIndented { render() }
            printer.print("}")
        }

        private fun renderInChildrenSection(render: () -> Unit) {
            printer.printIndented { render() }
        }
    }

    private class FirVerbosePrinter(private val builder: StringBuilder) {
        private var indentation: Int = 0

        fun print(text: String) {
            builder.append(" ".repeat(indentation))
            builder.append(text)
            builder.append("\n")
        }

        fun printIndented(print: () -> Unit) {
            indentation += 1
            print()
            indentation -= 1
        }
    }
}

