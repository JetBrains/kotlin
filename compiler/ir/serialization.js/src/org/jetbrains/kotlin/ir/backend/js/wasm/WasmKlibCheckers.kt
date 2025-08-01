/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibDiagnosticContext
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibExportingDeclaration
import org.jetbrains.kotlin.ir.backend.js.wasm.declarations.WasmKlibExportsChecker
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.library.SerializedIrFile

object WasmKlibCheckers {

    private val exportedDeclarationsCheckers = listOf(
        WasmKlibExportsChecker
    )

    fun makeChecker(
        diagnosticReporter: IrDiagnosticReporter,
        configuration: CompilerConfiguration,
        cleanFiles: List<SerializedIrFile> = listOf(),
        exportedNames: Map<IrFile, Map<IrDeclarationWithName, String>> = mapOf(),
    ): IrVisitorVoid {
        return object : IrVisitorVoid() {
            private val diagnosticContext = JsKlibDiagnosticContext(configuration)

            override fun visitElement(element: IrElement) {
                if (element is IrDeclaration) {
                    diagnosticContext.withDeclarationScope(element) {
                        element.acceptChildrenVoid(this)
                    }
                } else {
                    element.acceptChildrenVoid(this)
                }
            }

            override fun visitModuleFragment(declaration: IrModuleFragment) {
                val exportedDeclarations = JsKlibExportingDeclaration.collectDeclarations(cleanFiles, declaration.files, exportedNames)
                for (checker in exportedDeclarationsCheckers) {
                    checker.check(exportedDeclarations, this.diagnosticContext, diagnosticReporter)
                }
                super.visitModuleFragment(declaration)
            }

            override fun visitFile(declaration: IrFile) {
                diagnosticContext.withFileScope(declaration) {
                    super.visitFile(declaration)
                }
            }

        }
    }

}