/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.checkers

import org.jetbrains.kotlin.backend.common.checkers.CommonKlibDiagnosticContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

object NativeKlibCheckers {
    private val callCheckers: List<NativeKlibExpressionsChecker<IrCall>> = listOf(NativeVolatileCheck)

    fun makeChecker(
        diagnosticReporter: IrDiagnosticReporter,
        configuration: CompilerConfiguration,
    ): IrVisitorVoid {
        return object : IrVisitorVoid() {
            private val diagnosticContext = CommonKlibDiagnosticContext(configuration)

            override fun visitElement(element: IrElement) {
                if (element is IrDeclaration) {
                    diagnosticContext.withDeclarationScope(element) {
                        element.acceptChildrenVoid(this)
                    }
                } else {
                    element.acceptChildrenVoid(this)
                }
            }

            override fun visitFile(declaration: IrFile) {
                diagnosticContext.withFileScope(declaration) {
                    super.visitFile(declaration)
                }
            }

            override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
                diagnosticContext.withInlineScope(inlinedBlock) {
                    super.visitInlinedFunctionBlock(inlinedBlock)
                }
            }

            override fun visitCall(expression: IrCall) {
                for (checker in callCheckers) {
                    checker.check(expression, this.diagnosticContext, diagnosticReporter)
                }
                super.visitCall(expression)
            }
        }
    }
}
