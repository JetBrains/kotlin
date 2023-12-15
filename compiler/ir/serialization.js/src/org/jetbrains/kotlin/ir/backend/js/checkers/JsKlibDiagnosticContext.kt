/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.KtDiagnosticReporterWithContext
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile

class JsKlibDiagnosticContext(val compilerConfiguration: CompilerConfiguration) {
    var containingDeclaration: IrDeclaration? = null
        private set

    var containingFile: IrFile? = null
        private set

    fun withDeclarationScope(declaration: IrDeclaration, f: () -> Unit) {
        val prevDeclaration = containingDeclaration
        try {
            containingDeclaration = declaration
            f()
        } finally {
            containingDeclaration = prevDeclaration
        }
    }

    fun withFileScope(file: IrFile, f: () -> Unit) {
        val prevFile = containingFile
        try {
            containingFile = file
            f()
        } finally {
            containingFile = prevFile
        }
    }
}

fun IrDiagnosticReporter.at(
    declaration: IrDeclaration,
    context: JsKlibDiagnosticContext,
): KtDiagnosticReporterWithContext.DiagnosticContextImpl {
    return context.containingFile?.let { at(declaration, it) } ?: at(declaration)
}

fun IrDiagnosticReporter.at(
    irElement: IrElement,
    context: JsKlibDiagnosticContext,
): KtDiagnosticReporterWithContext.DiagnosticContextImpl {
    val file = context.containingFile
    if (file != null) {
        return at(irElement, file)
    }

    val declaration = context.containingDeclaration
    if (declaration != null) {
        return at(irElement, declaration)
    }

    // Should never happen
    error("Cannot find the expression containing declaration")
}
