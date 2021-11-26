/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.utils.DFS
import java.security.MessageDigest

fun ByteArray.md5(): Hash {
    val d = MessageDigest.getInstance("MD5").digest(this)!!
    return ((d[0].toLong() and 0xFFL)
            or ((d[1].toLong() and 0xFFL) shl 8)
            or ((d[2].toLong() and 0xFFL) shl 16)
            or ((d[3].toLong() and 0xFFL) shl 24)
            or ((d[4].toLong() and 0xFFL) shl 32)
            or ((d[5].toLong() and 0xFFL) shl 40)
            or ((d[6].toLong() and 0xFFL) shl 48)
            or ((d[7].toLong() and 0xFFL) shl 56))
}

class InlineFunctionFlatHashBuilder : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }


    private val fileToHash: MutableMap<IrFile, MutableMap<IrSimpleFunction, FlatHash>> = mutableMapOf()

    private var currentMap: MutableMap<IrSimpleFunction, FlatHash>? = null

    override fun visitFile(declaration: IrFile) {
        currentMap = fileToHash.getOrPut(declaration) { mutableMapOf() }
        declaration.acceptChildren(this, null)
        currentMap = null
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (declaration.isInline) {
            val m = currentMap ?: error("No graph map set for ${declaration.render()}")
            m[declaration] = declaration.dump().toByteArray().md5()
        }
        // do not go deeper since local declaration cannot be public api
    }

    val idToHashMap: Map<IrSimpleFunction, FlatHash> get() = fileToHash.values.flatMap { it.entries }.map { it.key to it.value }.toMap()

}

interface InlineFunctionHashProvider {
    fun hashForExternalFunction(declaration: IrSimpleFunction): TransHash?
}

class InlineFunctionHashBuilder(
    private val hashProvider: InlineFunctionHashProvider,
    private val flatHashes: Map<IrSimpleFunction, FlatHash>
) {
    private val inlineGraph: MutableMap<IrSimpleFunction, Set<IrSimpleFunction>> = mutableMapOf()

    private inner class GraphBuilder : IrElementVisitor<Unit, MutableSet<IrSimpleFunction>> {

        override fun visitElement(element: IrElement, data: MutableSet<IrSimpleFunction>) {
            element.acceptChildren(this, data)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: MutableSet<IrSimpleFunction>) {
            val newGraph = mutableSetOf<IrSimpleFunction>()
            inlineGraph[declaration] = newGraph
            declaration.acceptChildren(this, newGraph)
        }


        override fun visitCall(expression: IrCall, data: MutableSet<IrSimpleFunction>) {
            val callee = expression.symbol.owner

            if (callee.isInline) {
                data.add(callee)
            }

            expression.acceptChildren(this, data)
        }
    }


    private fun topologicalOrder(): List<IrSimpleFunction> {
        return DFS.topologicalOrder(inlineGraph.keys) {
            inlineGraph[it]?.filter { f -> f in inlineGraph } ?: run {
//                assert() not in current module
                emptySet()
            }
        }
    }

    private fun checkCircles() {

        val visited = mutableSetOf<IrSimpleFunction>()

        // TODO: check whether algorithm is correct
        for (f in inlineGraph.keys) {

            fun walk(current: IrSimpleFunction) {
                if (!visited.add(current)) {
                    error("Inline circle detected: ${current.render()} into ${f.render()}")
                }

                inlineGraph[current]?.let {
                    it.forEach { callee -> walk(callee) }
                }

                visited.remove(current)
            }

            walk(f)

            assert(visited.isEmpty())
        }
    }


    fun buildHashes(dirtyFiles: Collection<IrFile>): Map<IrSimpleFunction, TransHash> {

        dirtyFiles.forEach { it.acceptChildren(GraphBuilder(), mutableSetOf()) }

        checkCircles()

        val rpo = topologicalOrder()

        val computedHashes = mutableMapOf<IrSimpleFunction, TransHash>()

        fun transHash(callee: IrSimpleFunction): TransHash {
            return computedHashes[callee] ?: hashProvider.hashForExternalFunction(callee)
            ?: error("Internal error: No has found for ${callee.render()}")
        }

        for (f in rpo.asReversed()) {
            if (!f.isInline) continue
            val stringHash = buildString {
                val callees = inlineGraph[f]
                    ?: error("Expected to be in")
                // TODO: should it be a kind of stable order?
                for (callee in callees) {
                    val hash = transHash(callee)
                    append(hash.toString(Character.MAX_RADIX))
                }

                append(flatHashes[f] ?: error("Internal error: No flat hash for ${f.render()}"))
            }

            computedHashes[f] = stringHash.toByteArray().md5()
        }

        return computedHashes
    }

    fun buildInlineGraph(computedHashed: Map<IrSimpleFunction, TransHash>): Map<IrFile, Collection<Pair<IdSignature, TransHash>>> {
        val perFileInlineGraph = inlineGraph.entries.groupBy({ it.key.file }) {
            it.value
        }

        return perFileInlineGraph.map {
            it.key to it.value.flatMap { edges ->
                edges.mapNotNull { callee ->
                    // TODO: use resolved FO
                    if (!callee.isFakeOverride) {
                        val signature = callee.symbol.signature // ?: error("Expecting signature for ${callee.render()}")
                        if (signature?.visibleCrossFile == true) {
                            signature to (computedHashed[callee] ?: hashProvider.hashForExternalFunction(callee)
                            ?: error("Internal error: No has found for ${callee.render()}"))
                        } else null
                    } else null
                }
            }
        }.toMap()
    }
}
