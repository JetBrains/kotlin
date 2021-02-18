/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import java.io.File


class InlineGraphTracker : IrElementVisitor<Unit, String?> {
    override fun visitElement(element: IrElement, data: String?) {
        element.acceptChildren(this, data)
    }

    override fun visitFile(declaration: IrFile, data: String?) {
        val path = declaration.fileEntry.name
        currentFileSet = fileUsageMap.getOrPut(path) { mutableSetOf() }
        declaration.acceptChildren(this, path)
        currentFileSet = null
    }

    override fun visitCall(expression: IrCall, data: String?) {
        super.visitCall(expression, data)

        val callee = expression.symbol.owner

        if (callee.isInline) {
            val calleePath = callee.file.fileEntry.name
            if (calleePath != data) {
                val fileSet = currentFileSet ?: error("...")
                fileSet.add(calleePath)
            }
        }
    }

    private var currentFileSet: MutableSet<String>? = null
    private val fileUsageMap: MutableMap<String, MutableSet<String>> = mutableMapOf()

    operator fun get(path: String): Set<String> = fileUsageMap[path] ?: emptySet()

    fun invalidateForFile(path: String) { fileUsageMap.remove(path) }
}

class InlineGraphTracker2 : IrElementVisitorVoid {


    class GraphEdge(val from: String, val to: String) {

        constructor(fileFrom: IrFile, fileTo: IrFile) : this(fileFrom.fileEntry.name, fileTo.fileEntry.name)

        override fun equals(other: Any?): Boolean {
            return other is GraphEdge && from == other.from && to == other.to
        }

        override fun hashCode(): Int {
            return from.hashCode() xor to.hashCode()
        }

        override fun toString(): String {
            return buildString {
                append(from)
                append(" -> ")
                append(to)
            }
        }

        fun isEdgeFor(f: IrFile): Boolean = from == f.fileEntry.name || to == f.fileEntry.name
    }


    fun dump(): String {
        return buildString {
            appendLine("Inline Graph [")
            graph.forEach {
                append("\t")
                appendLine(it.toString())
            }
            appendLine("]")
        }
    }

    private val graph = mutableSetOf<GraphEdge>()

    private var currentTo: IrFile? = null

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }


    override fun visitFile(declaration: IrFile) {
        enterFile(declaration)
        super.visitFile(declaration)
    }


    override fun visitCall(expression: IrCall) {

        val callee = expression.symbol.owner

        if (callee.isInline) {
            trackFile(callee.file)
        }

        super.visitCall(expression)
    }

    fun enterFile(file: IrFile) {
        currentTo = file
    }

    fun trackFile(file: IrFile) {
        if (file !== currentTo) { // do not track itself
            graph.add(GraphEdge(file, currentTo ?: error("Has to be non-null")))
        }
    }

    fun copy(): InlineGraphTracker2 {
        return InlineGraphTracker2().also { it.graph.addAll(graph) }
    }

    fun invalidateForFile(path: String): Boolean {
        return graph.removeIf { it.to === path }
    }

    class InvalidationSet(private val from: String, val files: Set<String>) {
        override fun toString(): String {
            return buildString {
                appendLine("Invalidation set for ${File(from).canonicalPath} -> {")
                files.forEach { appendLine("\t${File(it).canonicalPath}") }
                appendLine("}")
            }
        }

        fun isEmpty(): Boolean = files.isEmpty()
    }

    fun getInvalidationSetForDirtyFile(path: String): InvalidationSet {
        val visited = mutableSetOf<String>()

        fun walk(f: String) {
            if (f in visited) return
            visited.add(f)
            graph.forEach { if (it.from == f) walk(it.to) }
        }

        walk(path)

        visited.remove(path)

        return InvalidationSet(path, visited)
    }
}