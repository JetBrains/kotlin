/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.backend.common.psi.PsiSourceManager
import org.jetbrains.kotlin.backend.common.sourceElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.file

class KtDiagnosticReporterWithImplicitIrBasedContext(
    diagnosticReporter: DiagnosticReporter,
    languageVersionSettings: LanguageVersionSettings
) : KtDiagnosticReporterWithContext(diagnosticReporter, languageVersionSettings) {

    fun at(irElement: IrElement, containingIrFile: IrFile): DiagnosticContextImpl =
        at(
            PsiSourceManager.findPsiElement(irElement, containingIrFile)?.let(::KtRealPsiSourceElement)
                ?: irElement.sourceElement(),
            containingIrFile.path
        )

    fun atFirstValidFrom(vararg irElements: IrElement, containingIrFile: IrFile): DiagnosticContextImpl {
        require(irElements.isNotEmpty())
        val sourceElement =
            irElements.firstNotNullOfOrNull { PsiSourceManager.findPsiElement(it, containingIrFile) }?.let(::KtRealPsiSourceElement)
                ?: (irElements.find { it.startOffset >= 0 } ?: irElements.first()).sourceElement()
        return at(sourceElement, containingIrFile.path)
    }

    fun at(irElement: IrElement, containingIrDeclaration: IrDeclaration): DiagnosticContextImpl =
        at(irElement, containingIrDeclaration.file)

    fun at(irDeclaration: IrDeclaration): DiagnosticContextImpl =
        at(irDeclaration, irDeclaration)
}

