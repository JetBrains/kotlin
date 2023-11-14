/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.KtDiagnosticReporterWithContext.DiagnosticContextImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile

interface IrDiagnosticReporter {
    val languageVersionSettings: LanguageVersionSettings
    fun at(irDeclaration: IrDeclaration): DiagnosticContextImpl
    fun at(irElement: IrElement, containingIrFile: IrFile): DiagnosticContextImpl
    fun at(irElement: IrElement, containingIrDeclaration: IrDeclaration): DiagnosticContextImpl
}