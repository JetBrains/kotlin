/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock

class CommonKlibDiagnosticContext(val compilerConfiguration: CompilerConfiguration) {
    var containingDeclaration: IrDeclaration? = null
        private set

    var containingFile: IrFile? = null
        private set

    val inlineBlockStack: List<IrInlinedFunctionBlock>
        field = mutableListOf()

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

    fun withInlineScope(inlineBlock: IrInlinedFunctionBlock, f: () -> Unit) {
        try {
            inlineBlockStack.add(inlineBlock)
            f()
        } finally {
            inlineBlockStack.removeLast()
        }
    }
}

fun IrDiagnosticReporter.at(
    declaration: IrDeclaration,
    context: CommonKlibDiagnosticContext,
): IrDiagnosticReporter.IrDiagnosticContext {
    return context.containingFile?.let { at(declaration, it) } ?: at(declaration)
}

fun IrDiagnosticReporter.at(
    irElement: IrElement,
    context: CommonKlibDiagnosticContext,
): IrDiagnosticReporter.IrDiagnosticContext {
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
