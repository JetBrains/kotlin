/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.ir.backend.js.optimizations.UnwrapCallableReferences
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

/**
 * An optimization step over the whole linked program, wrapped as a [NamedCompilerPhase] so
 * [PhaseConfig] can disable and profile it; [notEnabledOutput] substitutes the result when
 * the phase is disabled.
 */
private class WholeProgramOptimizationPhase<Input, Output>(
    name: String,
    private val notEnabledOutput: (JsIrBackendContext, Input) -> Output,
    private val body: (JsIrBackendContext, Input) -> Output,
) : NamedCompilerPhase<JsIrBackendContext, Input, Output>(name) {
    override fun phaseBody(context: JsIrBackendContext, input: Input): Output =
        body(context, input)

    override fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: JsIrBackendContext, input: Input): Output =
        notEnabledOutput(context, input)
}

private val buildJsModuleDFGPhase = WholeProgramOptimizationPhase<Iterable<IrModuleFragment>, JsModuleDFG>(
    name = "BuildJsModuleDFG",
    // An empty DFG turns every consumer into a no-op, so disabling this phase disables all
    // DFG-based optimizations at once.
    notEnabledOutput = { context, modules ->
        JsModuleDFG(modules, functions = mutableListOf(), symbolTable = JsDataFlowIR.SymbolTable(context))
    },
    body = { context, modules -> buildJsModuleDFG(context, modules) },
)

/**
 * An optimization consuming the shared [JsModuleDFG]. [body] returns `true` when the pass may
 * have left orphaned declarations behind, so the pipeline knows to rerun dead code elimination.
 * A pass that mutates a function's IR must also refresh that function in the DFG before
 * returning (see [buildJsModuleDFG]) — later phases in [dfgOptimizationPhases] read the same graph.
 */
private fun dfgOptimizationPhase(
    name: String,
    body: (JsIrBackendContext, JsModuleDFG) -> Boolean,
) = WholeProgramOptimizationPhase(name, notEnabledOutput = { _, _ -> false }, body = body)

/** DFG-based optimizations, in execution order. To add one, add an entry here. */
private val dfgOptimizationPhases = listOf(
    dfgOptimizationPhase("UnwrapCallableReferences") { context, dfg ->
        UnwrapCallableReferences(context).lower(dfg)
    },
)

/**
 * Whole-program IR optimizations for production builds.
 * Runs once over the fully lowered, linked program.
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
