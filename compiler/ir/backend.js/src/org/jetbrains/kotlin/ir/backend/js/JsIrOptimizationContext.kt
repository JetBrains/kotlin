/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsModuleDFG
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.buildJsModuleDFG
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class JsIrOptimizationContext(
    val backendContext: JsIrBackendContext,
    dceRoots: Iterable<IrDeclaration>,
    modules: Iterable<IrModuleFragment>,
) : LoweringContext by backendContext {
    val dceRoots = dceRoots.toMutableSet()

    /**
     * The program-wide DFG snapshot shared by [JsOptimizationPhase.DataFlow] phases.
     *
     * It is built on first use, after all preceding optimization phases have run.
     */
    internal val dataFlowGraph: Lazy<JsModuleDFG> = lazy(LazyThreadSafetyMode.NONE) { buildJsModuleDFG(backendContext, modules) }
}
