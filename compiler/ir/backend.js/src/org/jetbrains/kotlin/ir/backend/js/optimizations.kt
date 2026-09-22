/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.buildRoots
import org.jetbrains.kotlin.ir.backend.js.dce.eliminateDeadDeclarations
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
 * Runs whole-program IR optimizations once over the fully lowered, linked program.
 *
 * Initial dead code elimination prepares the IR before [optimizationLoweringList] runs. Module
 * phases and program-wide DFG phases then execute in list order. Consecutive DFG phases share one
 * lazily built snapshot and are followed by dead code elimination when any of them reports newly
 * unreachable declarations. A final dead code elimination removes what the optimizations orphaned.
 */
fun optimizeProgramByIr(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext,
    moduleKind: ModuleKind,
) {
    val allRoots = buildRoots(modules, context, moduleKind)
    val dceDumpNameCache = DceDumpNameCache() // in JS mode only DCE Graph could be dumped
    val phaserState = PhaserState()
    val optimizationContext = JsIrOptimizationContext(context, allRoots, modules)

    fun eliminateDeadDeclarations() {
        eliminateDeadDeclarations(optimizationContext.dceRoots, modules, context, dceDumpNameCache)
    }

    eliminateDeadDeclarations()
    var hasOrphanedDeclarations = false
    for (optimization in optimizationLoweringList) {
        when (optimization) {
            is JsOptimizationPhase.Module -> {
                if (hasOrphanedDeclarations) {
                    eliminateDeadDeclarations()
                    hasOrphanedDeclarations = false
                }
                for (module in modules) {
                    optimization.phase.invoke(context.phaseConfig, phaserState, optimizationContext, module)
                }
            }
            is JsOptimizationPhase.DataFlow -> {
                val orphaned = optimization.phase.invoke(
                    context.phaseConfig, phaserState, optimizationContext, input = optimizationContext.dataFlowGraph,
                )
                hasOrphanedDeclarations = hasOrphanedDeclarations || orphaned
            }
        }
    }

    // The optimizations may have rendered more declarations unreachable, run a second DCE pass to eliminate those.
    eliminateDeadDeclarations()
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
