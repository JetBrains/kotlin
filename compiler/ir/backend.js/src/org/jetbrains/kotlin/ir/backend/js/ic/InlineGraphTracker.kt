/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid


class InlineFunctionFlatHashBuilder : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (declaration.isInline) {
            flatHashes[declaration] = declaration.irElementHashForIC()
        }
        // go deeper since local inline special declarations (like a reference adaptor) may appear
        declaration.acceptChildren(this, null)
    }

    private val flatHashes = mutableMapOf<IrFunction, ICHash>()

    fun getFlatHashes() = flatHashes
}

interface InlineFunctionHashProvider {
    fun hashForExternalFunction(declaration: IrFunction): ICHash?
}

class InlineFunctionHashBuilder(
    private val hashProvider: InlineFunctionHashProvider,
    private val flatHashes: Map<IrFunction, ICHash>
) {
    private val inlineFunctionCallGraph: MutableMap<IrFunction, Set<IrFunction>> = mutableMapOf()

    private inner class GraphBuilder : IrElementVisitor<Unit, MutableSet<IrFunction>> {
        var inlineFunctionCallDepth: Int = 0

        override fun visitElement(element: IrElement, data: MutableSet<IrFunction>) {
            element.acceptChildren(this, data)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: MutableSet<IrFunction>) {
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
                    error("Internal error: inline function calls depth inconsistency")
                }
            }
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: MutableSet<IrFunction>) {
            val reference = expression.symbol.owner
            if (inlineFunctionCallDepth > 0 && reference.isInline) {
                data += reference
            }
            expression.acceptChildren(this, data)
        }
    }

    private inner class InlineFunctionHashProcessor {
        private val computedHashes = mutableMapOf<IrFunction, ICHash>()
        private val processingFunctions = mutableSetOf<IrFunction>()

        private fun processInlineFunction(f: IrFunction): ICHash = computedHashes.getOrPut(f) {
            if (!processingFunctions.add(f)) {
                error("Inline circle through function ${f.render()} detected")
            }
            val callees = inlineFunctionCallGraph[f] ?: error("Internal error: Inline function is missed in inline graph ${f.render()}")
            val flatHash = flatHashes[f] ?: error("Internal error: No flat hash for ${f.render()}")
            var functionInlineHash = flatHash
            for (callee in callees) {
                functionInlineHash = functionInlineHash.combineWith(processCallee(callee))
            }
            processingFunctions.remove(f)
            functionInlineHash
        }

        private fun processCallee(callee: IrFunction): ICHash {
            if (callee in flatHashes) {
                return processInlineFunction(callee)
            }
            return hashProvider.hashForExternalFunction(callee) ?: error("Internal error: No hash found for ${callee.render()}")
        }

        fun process(): Map<IrFunction, ICHash> {
            for ((f, callees) in inlineFunctionCallGraph.entries) {
                if (f.isInline) {
                    processInlineFunction(f)
                } else {
                    callees.forEach(::processCallee)
                }
            }
            return computedHashes
        }
    }

    fun buildHashes(dirtyFiles: Collection<IrFile>): Map<IrFunction, ICHash> {
        dirtyFiles.forEach { it.acceptChildren(GraphBuilder(), mutableSetOf()) }
        return InlineFunctionHashProcessor().process()
    }

    fun buildInlineGraph(computedHashed: Map<IrFunction, ICHash>): Map<IrFile, Map<IdSignature, ICHash>> {
        val perFileInlineGraph = inlineFunctionCallGraph.entries.groupBy({ it.key.file }) {
            it.value
        }

        return perFileInlineGraph.entries.associate {
            val usedInlineFunctions = mutableMapOf<IdSignature, ICHash>()
            it.value.forEach { edges ->
                edges.forEach { callee ->
                    if (!callee.isFakeOverride) {
                        val signature = callee.symbol.signature // ?: error("Expecting signature for ${callee.render()}")
                        if (signature?.visibleCrossFile == true) {
                            val calleeHash = computedHashed[callee]
                                ?: hashProvider.hashForExternalFunction(callee)
                                ?: error("Internal error: No has found for ${callee.render()}")
                            usedInlineFunctions[signature] = calleeHash
                        }
                    }
                }
            }
            it.key to usedInlineFunctions
        }
    }
}
