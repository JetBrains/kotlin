/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.backend.common.sourceElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.file

class KtDiagnosticReporterWithImplicitIrBasedContext(
    val diagnosticReporter: DiagnosticReporter,
    val languageVersionSettings: LanguageVersionSettings
) : DiagnosticReporter()  {
    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) = diagnosticReporter.report(diagnostic, context)

    fun at(irElement: IrElement, containingIrFile: IrFile): DiagnosticContextOverIr =
        DiagnosticContextOverIr(irElement, containingIrFile)

    fun at(irElement: IrElement, containingIrDeclaration: IrDeclaration): DiagnosticContextOverIr =
        DiagnosticContextOverIr(irElement, containingIrDeclaration)

    fun at(irDeclaration: IrDeclaration): DiagnosticContextOverIr =
        DiagnosticContextOverIr(irDeclaration, irDeclaration)

    @Suppress("UNUSED_PARAMETER")
    inner class DiagnosticContextOverIr(irElement: IrElement, containingIrFile: IrFile) : DiagnosticContext {

        constructor(irElement: IrElement, containingIrDeclaration: IrDeclaration): this(irElement, containingIrDeclaration.file)

        val sourceElement = irElement.sourceElement()

        override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean {
            TODO("Not yet implemented")
        }

        override val languageVersionSettings: LanguageVersionSettings
            get() = this@KtDiagnosticReporterWithImplicitIrBasedContext.languageVersionSettings

        @OptIn(InternalDiagnosticFactoryMethod::class)
        fun report(
            factory: KtDiagnosticFactory0,
            positioningStrategy: AbstractSourceElementPositioningStrategy? = null
        ) {
            sourceElement?.let { report(factory.on(it, positioningStrategy), this) }
        }

        @OptIn(InternalDiagnosticFactoryMethod::class)
        fun <A : Any> report(
            factory: KtDiagnosticFactory1<A>,
            a: A,
            positioningStrategy: AbstractSourceElementPositioningStrategy? = null
        ) {
            sourceElement?.let { report(factory.on(it, a, positioningStrategy), this) }
        }
    }
}

