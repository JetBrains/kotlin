/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid


class InlineFunctionTransitiveHashCalculator {
    private val flatHashes = mutableMapOf<IrFunction, ICHash>()
    private val inlineFunctionCallGraph: MutableMap<IrFunction, Set<IrFunction>> = mutableMapOf()
    private val processingFunctions = mutableSetOf<IrFunction>()
    private val functionTransitiveHashes = mutableMapOf<IrFunction, ICHash>()
    private val idSignatureTransitiveHashes = mutableMapOf<IdSignature, ICHash>()

    val transitiveHashes: Map<IdSignature, ICHash>
        get() = idSignatureTransitiveHashes

    private inner class FlatHashCalculator : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) = element.acceptChildren(this, null)

        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            if (declaration.isInline) {
                if (declaration in flatHashes) {
                    return
                }

                flatHashes[declaration] = if (declaration.isFakeOverride) {
                    declaration.resolveFakeOverride()?.irElementHashForIC()
                        ?: icError("can not resolve fake override for ${declaration.render()}")
                } else {
                    declaration.irElementHashForIC()
                }
            }
            // go deeper since local inline special declarations (like a reference adaptor) may appear
            declaration.acceptChildren(this, null)
        }
    }

    private inner class CallGraphBuilder : IrElementVisitor<Unit, MutableSet<IrFunction>> {
        var inlineFunctionCallDepth: Int = 0

        override fun visitElement(element: IrElement, data: MutableSet<IrFunction>) = element.acceptChildren(this, data)

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: MutableSet<IrFunction>) {
            if (declaration in inlineFunctionCallGraph) {
                return
            }
            val newGraph = mutableSetOf<IrFunction>()
            inlineFunctionCallGraph[declaration] = newGraph
            declaration.acceptChildren(this, newGraph)
        }

        override fun visitCall(expression: IrCall, data: MutableSet<IrFunction>) {
            val callee = expression.symbol.owner
            if (callee.isInline) {
                data += callee
                inlineFunctionCallDepth += 1
            }
            expression.acceptChildren(this, data)
            if (callee.isInline) {
                inlineFunctionCallDepth -= 1
                if (inlineFunctionCallDepth < 0) {
                    icError("inline function calls depth inconsistent")
                }
            }
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: MutableSet<IrFunction>) {
            val reference = expression.symbol.owner
            if (inlineFunctionCallDepth > 0 && reference.isInline) {
                // this if is fine, because fake overrides are not inlined as function reference calls even as inline function args
                if (!reference.isFakeOverride) {
                    data += reference
                }
            }
            expression.acceptChildren(this, data)
        }
    }

    private fun getInlineFunctionTransitiveHash(f: IrFunction): ICHash = functionTransitiveHashes.getOrPut(f) {
        if (!processingFunctions.add(f)) {
            icError("inline circle through function ${f.render()} detected")
        }
        val callees = inlineFunctionCallGraph[f] ?: icError("inline function is missed in inline graph ${f.render()}")
        val flatHash = flatHashes[f] ?: icError("no flat hash for ${f.render()}")
        var functionInlineHash = flatHash
        for (callee in callees) {
            functionInlineHash = functionInlineHash.combineWith(getInlineFunctionTransitiveHash(callee))
        }
        processingFunctions.remove(f)
        f.symbol.signature?.let { idSignatureTransitiveHashes[it] = functionInlineHash }
        functionInlineHash
    }

    private fun updateTransitiveHashesByCallGraph() {
        for ((f, callees) in inlineFunctionCallGraph.entries) {
            if (f.isInline) {
                getInlineFunctionTransitiveHash(f)
            } else {
                callees.forEach(::getInlineFunctionTransitiveHash)
            }
        }
    }

    fun updateTransitiveHashes(fragments: Collection<IrModuleFragment>) {
        fragments.forEach { it.acceptVoid(FlatHashCalculator()) }
        fragments.forEach { it.acceptChildren(CallGraphBuilder(), mutableSetOf()) }
        updateTransitiveHashesByCallGraph()
    }
}
