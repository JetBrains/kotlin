/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.utils.DFS
import java.security.MessageDigest

fun ByteArray.md5(): Long {
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


    private val fileToHash: MutableMap<IrFile, MutableMap<IrSimpleFunction, Long>> = mutableMapOf()

    private var currentMap: MutableMap<IrSimpleFunction, Long>? = null

    override fun visitFile(declaration: IrFile) {
        currentMap = fileToHash.getOrPut(declaration) { mutableMapOf() }
        declaration.acceptChildren(this, null)
        currentMap = null
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (declaration.isInline) {
            val m = currentMap!!
            m[declaration] = declaration.dump().toByteArray().md5()
        }
        // do not go deeper since local declaration cannot be public api
    }

    val idToHashMap: Map<IrSimpleFunction, Long> get() = fileToHash.values.flatMap { it.entries }.map { it.key to it.value }.toMap()

}

interface InlineFunctionHashProvider {
    fun hashForExternalFunction(declaration: IrSimpleFunction): Long?


}



class InlineFunctionHashBuilder(
    private val hashProvider: InlineFunctionHashProvider,
    private val flatHashes: Map<IrSimpleFunction, Long>
) {
    private val inlineGraph: MutableMap<IrSimpleFunction, Set<IrSimpleFunction>> = mutableMapOf()

    private inner class GraphBuilder : IrElementVisitor<Unit, MutableSet<IrSimpleFunction>> {

        override fun visitElement(element: IrElement, data: MutableSet<IrSimpleFunction>) {
            element.acceptChildren(this, data)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: MutableSet<IrSimpleFunction>) {
            if (declaration.isInline) {
                val newGraph = mutableSetOf<IrSimpleFunction>()
                inlineGraph[declaration] = newGraph
                declaration.acceptChildren(this, newGraph)
            }
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
            inlineGraph[it] ?: run {
//                assert() not in current module
                emptySet()
            }
        }
    }

    private fun checkCircles() {

        val visited = mutableSetOf<IrSimpleFunction>()
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


    fun buildHashes(dirtyFiles: Collection<IrFile>): Map<IrSimpleFunction, Long> {

        dirtyFiles.forEach { it.acceptChildren(GraphBuilder(), mutableSetOf()) }

        checkCircles()

        val rpo = topologicalOrder()

        val computedHashes = mutableMapOf<IrSimpleFunction, Long>()

        for (f in rpo) {
            val stringHash = buildString {
                val callees = inlineGraph[f] ?: error("Expected to be in")
                // TODO: should it be a kind of stable order?
                for (callee in callees) {
                    val hash = computedHashes[callee] ?: hashProvider.hashForExternalFunction(callee)
                    ?: error("Internal error: No has found for ${callee.render()}")
                    append(hash.toString(Character.MAX_RADIX))
                }

                append(flatHashes[f] ?: error("Internal error: No flat hash for ${f.render()}"))
            }

            computedHashes[f] = stringHash.toByteArray().md5()
        }

        return computedHashes
    }
}



//class InlineGraphTracker : IrElementVisitor<Unit, String?> {
//    override fun visitElement(element: IrElement, data: String?) {
//        element.acceptChildren(this, data)
//    }
//
//    override fun visitFile(declaration: IrFile, data: String?) {
//        val path = declaration.fileEntry.name
//        currentFileSet = fileUsageMap.getOrPut(path) { mutableSetOf() }
//        declaration.acceptChildren(this, path)
//        currentFileSet = null
//    }
//
//    override fun visitCall(expression: IrCall, data: String?) {
//        super.visitCall(expression, data)
//
//        val callee = expression.symbol.owner
//
//        if (callee.isInline) {
//            val calleePath = callee.file.fileEntry.name
//            if (calleePath != data) {
//                val fileSet = currentFileSet ?: error("...")
//                fileSet.add(calleePath)
//            }
//        }
//    }
//
//    private var currentFileSet: MutableSet<String>? = null
//    private val fileUsageMap: MutableMap<String, MutableSet<String>> = mutableMapOf()
//
//    operator fun get(path: String): Set<String> = fileUsageMap[path] ?: emptySet()
//
//    fun invalidateForFile(path: String) { fileUsageMap.remove(path) }
//}

//class InlineGraphTracker2 : IrElementVisitorVoid {
//
//
//    class GraphEdge(val from: String, val to: String) {
//
//        constructor(fileFrom: IrFile, fileTo: IrFile) : this(fileFrom.fileEntry.name, fileTo.fileEntry.name)
//
//        override fun equals(other: Any?): Boolean {
//            return other is GraphEdge && from == other.from && to == other.to
//        }
//
//        override fun hashCode(): Int {
//            return from.hashCode() xor to.hashCode()
//        }
//
//        override fun toString(): String {
//            return buildString {
//                append(from)
//                append(" -> ")
//                append(to)
//            }
//        }
//
//        fun isEdgeFor(f: IrFile): Boolean = from == f.fileEntry.name || to == f.fileEntry.name
//    }
//
//
//    fun dump(): String {
//        return buildString {
//            appendLine("Inline Graph [")
//            graph.forEach {
//                append("\t")
//                appendLine(it.toString())
//            }
//            appendLine("]")
//        }
//    }
//
//    private val graph = mutableSetOf<GraphEdge>()
//
//    private var currentTo: IrFile? = null
//
//    override fun visitElement(element: IrElement) {
//        element.acceptChildren(this, null)
//    }
//
//
//    override fun visitFile(declaration: IrFile) {
//        enterFile(declaration)
//        super.visitFile(declaration)
//    }
//
//
//    override fun visitCall(expression: IrCall) {
//
//        val callee = expression.symbol.owner
//
//        if (callee.isInline) {
//            trackFile(callee.file)
//        }
//
//        super.visitCall(expression)
//    }
//
//    fun enterFile(file: IrFile) {
//        currentTo = file
//    }
//
//    fun trackFile(file: IrFile) {
//        if (file !== currentTo) { // do not track itself
//            graph.add(GraphEdge(file, currentTo ?: error("Has to be non-null")))
//        }
//    }
//
//    fun copy(): InlineGraphTracker2 {
//        return InlineGraphTracker2().also { it.graph.addAll(graph) }
//    }
//
//    fun invalidateForFile(path: String): Boolean {
//        return graph.removeIf { it.to === path }
//    }
//
//    class InvalidationSet(private val from: String, val files: Set<String>) {
//        override fun toString(): String {
//            return buildString {
//                appendLine("Invalidation set for ${File(from).canonicalPath} -> {")
//                files.forEach { appendLine("\t${File(it).canonicalPath}") }
//                appendLine("}")
//            }
//        }
//
//        fun isEmpty(): Boolean = files.isEmpty()
//    }
//
//    fun getInvalidationSetForDirtyFile(path: String): InvalidationSet {
//        val visited = mutableSetOf<String>()
//
//        fun walk(f: String) {
//            if (f in visited) return
//            visited.add(f)
//            graph.forEach { if (it.from == f) walk(it.to) }
//        }
//
//        walk(path)
//
//        visited.remove(path)
//
//        return InvalidationSet(path, visited)
//    }
//}