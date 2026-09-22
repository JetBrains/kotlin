/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/** Linked-program DFG: analyzed [executionUnits], their [modules], and the shared [symbolTable]. */
class JsModuleDFG(
    val modules: Iterable<IrModuleFragment>,
    val executionUnits: MutableList<JsDataFlowIR.ExecutionUnit>,
    val symbolTable: JsDataFlowIR.SymbolTable,
)

/** Populates and seals a [JsDataFlowIR.SymbolTable], then builds every executable IR body. */
internal class JsModuleDFGBuilder(
    val context: JsIrBackendContext,
    val irModules: Iterable<IrModuleFragment>,
) {
    private val symbolTable = JsDataFlowIR.SymbolTable(context)

    fun build(): JsModuleDFG {
        for (irModule in irModules) {
            symbolTable.populateWith(irModule)
        }

        val executionUnits = mutableListOf<JsDataFlowIR.ExecutionUnit>()
        val visitor = object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                val body = declaration.body
                if (body == null) {
                    symbolTable.mapFunction(declaration)
                } else {
                    analyze(declaration)
                }
                // Lambdas and local classes nested in the body get their own DFG functions.
                declaration.acceptChildrenVoid(this)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                val body = declaration.body
                if (body == null) {
                    symbolTable.mapFunction(declaration)
                } else {
                    analyze(declaration)
                }
                declaration.acceptChildrenVoid(this)
            }

            override fun visitField(declaration: IrField) {
                if (declaration.isStatic && declaration.initializer != null) {
                    analyze(declaration)
                }
                declaration.acceptChildrenVoid(this)
            }

            private fun analyze(irFunction: IrFunction) {
                executionUnits += JsBodyDFGBuilder(context, symbolTable).buildFunction(irFunction)
            }

            private fun analyze(irField: IrField) {
                executionUnits += JsBodyDFGBuilder(context, symbolTable).buildInitializer(irField)
            }
        }

        for (irModule in irModules) {
            irModule.accept(visitor, data = null)
        }

        symbolTable.seal()
        return JsModuleDFG(irModules, executionUnits, symbolTable)
    }
}

/**
 * Walks every function and static field initializer in [modules] and returns one [JsModuleDFG].
 *
 * The result is a snapshot of the IR at build time.
 */
fun buildJsModuleDFG(
    context: JsIrBackendContext,
    modules: Iterable<IrModuleFragment>,
): JsModuleDFG = JsModuleDFGBuilder(context, modules).build()
