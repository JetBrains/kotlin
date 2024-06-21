/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

class DisjointUnions<T> {
    private val leafParents = mutableMapOf<T, Node>()
    private var dirty: Boolean = false

    private inner class Node(var rank: Int = 0, val leafs: MutableList<T> = mutableListOf()) {
        var parent: Node? = null
        override fun toString(): String = "${if (parent == null) "ROOT " else ""}Node with ${leafs.size} leafs and $rank rank"
    }

    private fun findRoot(node: T): Node? =
        leafParents[node]?.let { findRoot(it) }

    private fun findRoot(node: Node, pathWeight: Int = 0): Node {
        val strictParent = node.parent ?: return node
        val currentWeight = pathWeight + node.leafs.size
        val foundRoot = findRoot(strictParent, currentWeight)

        if (foundRoot != node) {
            val leafs = node.leafs
            node.rank -= currentWeight
            check(node.rank >= 0)
            foundRoot.leafs.addAll(leafs)
            leafs.clear()
            node.parent = foundRoot
        }

        return foundRoot
    }

    private fun addToRoot(leaf: T, root: Node) {
        leafParents[leaf] = root
        root.rank++
        root.leafs.add(leaf)
    }

    private fun mergeRoots(root1: Node, root2: Node): Node {
        if (root1 == root2) return root1
        require(root1.parent == null && root2.parent == null) { "Merge is possible only for root nodes" }
        if (root2.parent == root1) return root1
        if (root1.parent == root2) return root2

        val rootToMove: Node
        val newParentRoot: Node
        if (root1.rank > root2.rank) {
            rootToMove = root2
            newParentRoot = root1
        } else {
            rootToMove = root1
            newParentRoot = root2
        }

        rootToMove.parent = newParentRoot

        val leafs = rootToMove.leafs
        newParentRoot.rank += leafs.size
        rootToMove.rank -= leafs.size
        check(rootToMove.rank >= 0)
        newParentRoot.leafs.addAll(leafs)
        leafs.clear()

        return newParentRoot
    }

    fun addUnion(elements: List<T>) {
        var currentRoot: Node? = null
        dirty = true
        for (leaf in elements) {
            val strictRoot = leafParents[leaf]
            if (strictRoot == null) {
                currentRoot = currentRoot?.let(::findRoot) ?: Node()
                addToRoot(leaf, currentRoot)
            } else {
                val leafRoot = findRoot(strictRoot)
                currentRoot = if (currentRoot != null) mergeRoots(currentRoot, leafRoot) else leafRoot
            }
        }
    }

    fun compress() {
        if (dirty) {
            leafParents.keys.forEach(::findRoot)
            dirty = false
        }
    }

    operator fun contains(element: T): Boolean =
        leafParents.containsKey(element)

    operator fun get(element: T): List<T> {
        require(!dirty) { "Call compress before getting union" }
        val root = findRoot(element)
        require(root != null) { "Element not contains in any union" }
        check(root.rank == root.leafs.size) { "Invalid tree state after compress" }
        return root.leafs
    }

    fun allUnions(): List<List<T>> {
        require(!dirty) { "Call compress before getting union" }
        return leafParents.keys.map { get(it) }
    }
}