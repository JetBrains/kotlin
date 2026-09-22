/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.ir.backend.js.optimizations.UnwrapCallableReferences
import org.jetbrains.kotlin.ir.backend.js.optimizations.WholeProgramOptimizationPhase
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsModuleDFG
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.buildJsModuleDFG
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragment
import org.jetbrains.kotlin.ir.backend.js.utils.JsStaticContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.backend.ast.JsClass
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.inline.clean.ClassPostProcessor
import org.jetbrains.kotlin.js.inline.clean.FunctionPostProcessor

private val buildJsModuleDFGPhase = WholeProgramOptimizationPhase<Iterable<IrModuleFragment>, JsModuleDFG>(
    name = "BuildJsModuleDFG",
    // An empty snapshot makes every DFG consumer a no-op when graph construction is disabled.
    notEnabledOutput = { context, modules ->
        JsModuleDFG(modules, executionUnits = mutableListOf(), symbolTable = JsDataFlowIR.SymbolTable(context))
    },
    body = { context, modules -> buildJsModuleDFG(context, modules) },
)

/**
 * Wraps an optimization over the shared [JsModuleDFG] snapshot as a compiler phase.
 *
 * [body] returns true when its changes may have made declarations unreachable and therefore
 * require another dead code elimination pass. DFG phases run in [dfgOptimizationPhases] order
 * over the same snapshot. A phase that mutates relationships represented by the graph must
 * update them before a later phase relies on them, or be ordered after all such consumers.
 */
private fun dfgOptimizationPhase(
    name: String,
    body: (JsIrBackendContext, JsModuleDFG) -> Boolean,
) = WholeProgramOptimizationPhase(name, notEnabledOutput = { _, _ -> false }, body = body)

/** DFG consumers in execution order; every entry receives the same program-wide snapshot. */
private val dfgOptimizationPhases = listOf(
    dfgOptimizationPhase("UnwrapCallableReferences") { context, dfg ->
        UnwrapCallableReferences(context).lower(dfg)
    },
)

/**
 * Runs whole-program IR optimizations once over the fully lowered, linked program.
 *
 * Initial dead code elimination and callable-reference canonicalization prepare the IR before
 * the DFG snapshot is built. DFG consumers then run in sequence, followed by another dead code
 * elimination pass only when a consumer reports newly unreachable declarations. The remaining
 * per-module optimization lowerings run after the whole-program analysis.
 */
fun optimizeProgramByIr(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext,
    moduleKind: ModuleKind,
    removeUnusedAssociatedObjects: Boolean,
) {
    val phaserState = PhaserState()

    fun runModulePhases(phases: List<NamedCompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>>) {
        for (phase in phases) {
            for (module in modules) {
                phase.invoke(context.phaseConfig, phaserState, context, module)
            }
        }
    }

    fun eliminateDeadDeclarations() {
        val dceDumpNameCache = DceDumpNameCache() // in JS mode only DCE Graph could be dumped
        eliminateDeadDeclarations(modules, context, moduleKind, removeUnusedAssociatedObjects, dceDumpNameCache)
    }

    eliminateDeadDeclarations()
    runModulePhases(callableReferenceFactoryPhases)

    val dfg = buildJsModuleDFGPhase.invoke(context.phaseConfig, phaserState, context, input = modules)
    var hasOrphanedDeclarations = false
    for (phase in dfgOptimizationPhases) {
        val orphaned = phase.invoke(context.phaseConfig, phaserState, context, input = dfg)
        hasOrphanedDeclarations = hasOrphanedDeclarations || orphaned
    }
    if (hasOrphanedDeclarations) {
        eliminateDeadDeclarations()
    }

    runModulePhases(optimizationLoweringList)
}

fun optimizeFragmentByJsAst(fragment: JsIrProgramFragment, context: JsStaticContext) {
    val voidName = context.backendContext.symbols.void.owner.backingField?.let(context::getNameForField)

    val optimizer = object : RecursiveJsVisitor() {
        override fun visitFunction(x: JsFunction) {
            super.visitFunction(x)
            FunctionPostProcessor(x, voidName).apply()
        }

        override fun visitClass(x: JsClass) {
            super.visitClass(x)
            ClassPostProcessor(x).apply()
        }
    }

    fragment.declarations.statements.forEach { it.accept(optimizer) }
    fragment.classes.values.forEach { klass ->
        klass.postDeclarationBlock.statements.forEach { it.accept(optimizer) }
        klass.preDeclarationBlock.statements.forEach { it.accept(optimizer) }
    }
}
