/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.ir.backend.js.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.backend.ast.JsClass
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor
import org.jetbrains.kotlin.js.inline.clean.ClassPostProcessor
import org.jetbrains.kotlin.js.inline.clean.FunctionPostProcessor

fun optimizeProgramByIr(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext,
    removeUnusedAssociatedObjects: Boolean
) {
    eliminateDeadDeclarations(modules, context, removeUnusedAssociatedObjects)
    jsOptimizationPhases.invokeToplevel(PhaseConfig(jsOptimizationPhases), context, modules)
}

fun optimizeFragmentByJsAst(fragment: JsIrProgramFragment) {
    val optimizer = object : RecursiveJsVisitor() {
        override fun visitFunction(x: JsFunction) {
            FunctionPostProcessor(x).apply()
        }

        override fun visitClass(x: JsClass) {
            super.visitClass(x)
            ClassPostProcessor(x).apply()
        }
    }

    fragment.declarations.statements.forEach { it.accept(optimizer) }
    fragment.classes.values.forEach { klass ->
        klass.postDeclarationBlock.statements.forEach { it.accept(optimizer)}
        klass.preDeclarationBlock.statements.forEach { it.accept(optimizer)}
    }
}
