/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Thin linked-program index: functions, CFGs, and call sites.
 *
 * Not a per-expression data-flow graph. Intended as the shared substrate for concrete
 * type/value analysis, call-graph construction, and later inlining / devirtualization.
 */
class JsProgramIndex(
    val context: JsIrBackendContext,
    val modules: List<IrModuleFragment>,
    val functions: Map<IrFunction, JsIndexedFunction>,
    val callSites: List<JsCallSite>,
) {
    fun function(ir: IrFunction): JsIndexedFunction? = functions[ir]

    fun callsTo(callee: IrFunction): List<JsCallSite> =
        callSites.filter { it.directCallee == callee }
}

/**
 * Per-function view in the program index.
 *
 * @param hasUnknownCallers is true when parameters must not be refined from visible call sites
 * (exported, external, referenced as a function value, or open virtual target).
 */
class JsIndexedFunction(
    val ir: IrFunction,
    val cfg: JsControlFlowGraph,
    val hasUnknownCallers: Boolean,
)

/**
 * A direct call / constructor call with a resolved [directCallee] when statically known.
 */
class JsCallSite(
    val call: IrFunctionAccessExpression,
    val enclosingFunction: IrFunction?,
    val directCallee: IrFunction?,
)

/**
 * Builds [JsProgramIndex] for all functions with bodies in [modules].
 */
context(context: JsIrBackendContext)
fun buildJsProgramIndex(modules: Iterable<IrModuleFragment>): JsProgramIndex {
    // Skip stdlib / platform modules — analyses run on the linked *user* program.
    val moduleList = modules.filterNot { it.isKotlinLibraryModule() }
    val functionsWithBody = linkedMapOf<IrFunction, JsIndexedFunction>()
    val callSites = mutableListOf<JsCallSite>()
    val referencedAsValue = mutableSetOf<IrFunction>()
    val openCallees = mutableSetOf<IrFunction>()

    val callCollector = object : IrVisitorVoid() {
        private var currentFunction: IrFunction? = null

        override fun visitElement(element: IrElement) =
            element.acceptChildrenVoid(visitor = this)

        override fun visitFunction(declaration: IrFunction) {
            val previous = currentFunction
            currentFunction = declaration
            declaration.acceptChildrenVoid(visitor = this)
            currentFunction = previous
        }

        override fun visitCall(expression: IrCall) {
            recordCall(expression)
            expression.acceptChildrenVoid(visitor = this)
        }

        override fun visitConstructorCall(expression: IrConstructorCall) {
            recordCall(expression)
            expression.acceptChildrenVoid(visitor = this)
        }

        override fun visitFunctionReference(expression: IrFunctionReference) {
            referencedAsValue += expression.symbol.owner
            expression.acceptChildrenVoid(visitor = this)
        }

        private fun recordCall(expression: IrFunctionAccessExpression) {
            val directCallee = resolveDirectCallee(expression)
            if (directCallee == null) {
                openCallees += expression.symbol.owner
            }
            callSites += JsCallSite(expression, currentFunction, directCallee)
        }
    }

    for (module in moduleList) {
        for (file in module.files) {
            file.acceptVoid(callCollector)
        }
    }

    val functionIndexer = object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(visitor = this)
        }

        override fun visitFunction(declaration: IrFunction) {
            if (declaration.body != null) {
                val hasUnknownCallers = declaration.isEffectivelyExternal() ||
                        declaration.isExported(context) ||
                        declaration in referencedAsValue ||
                        declaration in openCallees ||
                        declaration.isOpenLike()
                functionsWithBody[declaration] = JsIndexedFunction(
                    ir = declaration,
                    cfg = buildJsFunctionCfg(declaration),
                    hasUnknownCallers = hasUnknownCallers,
                )
            }
            declaration.acceptChildrenVoid(visitor = this)
        }
    }

    for (module in moduleList) {
        module.acceptVoid(visitor = functionIndexer)
    }

    return JsProgramIndex(context, moduleList, functionsWithBody, callSites)
}

private fun resolveDirectCallee(expression: IrFunctionAccessExpression): IrFunction? {
    val callee = expression.symbol.owner
    return when (expression) {
        is IrConstructorCall -> callee
        is IrCall -> {
            if (expression.superQualifierSymbol != null) return callee
            val simple = callee as? IrSimpleFunction ?: return callee
            if (simple.isFakeOverride || simple.modality.isOpenLike()) null else simple
        }
        else -> callee
    }
}

private fun IrFunction.isOpenLike(): Boolean {
    val simple = this as? IrSimpleFunction ?: return false
    return simple.modality.isOpenLike()
}

private fun Modality.isOpenLike(): Boolean =
    this == Modality.OPEN || this == Modality.ABSTRACT || this == Modality.SEALED

private fun IrModuleFragment.isKotlinLibraryModule(): Boolean {
    val raw = name.asString().removeSurrounding("<", ">")
    return raw.startsWith("kotlin") || raw == "JS-all" || raw.contains("kotlin-stdlib")
}
