/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class SignaturesComputationLowering(val context: PreSerializationLoweringContext) : ModuleLoweringPass {
    private val declarationTable: DeclarationTable<*> = context.declarationTable

    // TODO: introduce non-void IrTreeSymbolsVisitor and include it in 'data'
    private var isDeclared = false

    private val visitor = object : IrTreeSymbolsVisitor() {
        override fun visitDeclaredSymbol(container: IrElement, symbol: IrSymbol) {
            val prev = isDeclared
            isDeclared = true
            super.visitDeclaredSymbol(container, symbol)
            isDeclared = prev
        }

        override fun visitReferencedSymbol(container: IrElement, symbol: IrSymbol) {
            val prev = isDeclared
            isDeclared = false
            super.visitReferencedSymbol(container, symbol)
            isDeclared = prev
        }

        override fun visitSymbol(container: IrElement, symbol: IrSymbol) {
            if (!symbol.isBound) return
            if (symbol is IrFileSymbol) return

            // Compute the signature:
            when (val symbolOwner = symbol.owner) {
                is IrDeclaration -> {
                    val record = when (symbolOwner) {
                        is IrSimpleFunction -> {
                            // Do not register prepared inline function copies in the clash detector.
                            val isPreparedCopy = symbolOwner.originalOfPreparedInlineFunctionCopy != null
                            isDeclared && !isPreparedCopy
                        }
                        else -> isDeclared
                    }
                    declarationTable.signatureByDeclaration(
                        declaration = symbolOwner,
                        compatibleMode = false,
                        recordInSignatureClashDetector = record,
                    )
                }
                is IrReturnableBlock -> declarationTable.signatureByReturnableBlock(symbolOwner)
                else -> error("Expected symbol owner: ${symbolOwner.render()}")
            }
        }

        override fun visitFile(declaration: IrFile) {
            declarationTable.inFile(declaration) {
                declaration.computeTopLevelDeclarationSignatures()
                super.visitFile(declaration)
            }
        }

        private fun IrFile.computeTopLevelDeclarationSignatures() = declarations.forEach {
            declarationTable.signatureByDeclaration(
                declaration = it,
                compatibleMode = false,
                recordInSignatureClashDetector = false,
            )
        }
    }

    override fun lower(irModule: IrModuleFragment) {
        // Traverse all files
        irModule.files.forEach { file ->
            file.acceptVoid(visitor)
        }

        // Compute signatures for prepared inline function copies created earlier
        irModule.preparedInlineFunctionCopies?.forEach { functionCopy ->
            val file = functionCopy.file
            declarationTable.inFile(file) {
                functionCopy.acceptVoid(visitor)
            }
            declarationTable.signatureByDeclaration(
                functionCopy.originalOfPreparedInlineFunctionCopy!!,
                compatibleMode = false,
                recordInSignatureClashDetector = false
            )
        }
    }
}
