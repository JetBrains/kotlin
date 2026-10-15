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

/**
 * A program-wide DFG snapshot over the linked IR [modules].
 *
 * Every function body and static field initializer is represented in [executionUnits]. All units
 * use [symbolTable], so references to the same IR declaration share an identity across modules.
 */
class JsModuleDFG(
    val modules: Iterable<IrModuleFragment>,
    val executionUnits: List<JsDataFlowIR.ExecutionUnit>,
    val symbolTable: JsDataFlowIR.SymbolTable,
) {
    /** Structural relationships within [executionUnits], shared by all DFG consumers. */
    internal val index = JsDataFlowIndex.from(executionUnits)
}

/**
 * Builds a [JsModuleDFG] in linked-program scope.
 *
 * The builder first pre-registers bodied callables and classes from every module. It then builds
 * each execution unit before visiting declarations nested inside it. This parent-first order lets
 * nested units reuse the nodes of captured parameters and locals through a construction-only
 * registry. The symbol table is sealed and the index is created only after all units, including
 * writes to captured variables, have been built.
 */
internal class JsModuleDFGBuilder(
    val context: JsIrBackendContext,
    val irModules: Iterable<IrModuleFragment>,
) {
    private val symbolTable = JsDataFlowIR.SymbolTable(context)

    /** Nodes available to nested execution units while this DFG snapshot is being built. */
    private val capturedValueNodes = mutableMapOf<IrValueDeclaration, JsDataFlowIR.Node>()

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
                // Continue traversal because nested declarations are separate execution units.
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
                executionUnits += JsBodyDFGBuilder(context, symbolTable, capturedValueNodes).buildFunction(irFunction)
            }

            private fun analyze(irField: IrField) {
                executionUnits += JsBodyDFGBuilder(context, symbolTable, capturedValueNodes).buildInitializer(irField)
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
 * Creates the shared data-flow snapshot consumed by whole-program JS optimizations.
 *
 * The result reflects [modules] at build time and is not updated automatically when a consumer
 * mutates the IR. A later graph consumer must therefore receive an updated graph or only inspect
 * portions that remain valid.
 */
fun buildJsModuleDFG(
    context: JsIrBackendContext,
    modules: Iterable<IrModuleFragment>,
): JsModuleDFG = JsModuleDFGBuilder(context, modules).build()
