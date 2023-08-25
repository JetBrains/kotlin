/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.Collection

class MetaInfoHierarchySet : AbstractMutableSet<ParsedCodeMetaInfo>() {
    open class NodeBase(
        val children: TreeMap<Int, Node>,
        var totalChildrenCount: Int,
    )

    class Node(
        val metaInfo: ParsedCodeMetaInfo,
        children: TreeMap<Int, Node>,
        totalChildrenCount: Int,
    ) : NodeBase(children, totalChildrenCount)

    var root = NodeBase(TreeMap(), 0)

    override val size get() = root.totalChildrenCount

    override fun add(element: ParsedCodeMetaInfo): Boolean {
        val parents = findParentsFor(element.start, element.end)
        val parent = parents.last()
        val iterator = parent.children.iterator()
        val innerNodes = TreeMap<Int, Node>()
        var totalChildrenCount = 0

        while (iterator.hasNext()) {
            val next = iterator.next().value

            // The second check accounts for the following corner case:
            // --------[)-------- <- next.metaInfo
            // --------[---)----- <- element
            // Since we use starts as keys we must
            // treat such overlaps as a parent-child
            // pair, not as siblings. So want to
            // rehang next.metaInfo to the element.
            if (next.metaInfo.end <= element.start && next.metaInfo.start < element.start) {
                continue
            }

            if (next.metaInfo.start < element.start) {
                error("Attempting to add an overlapping range. Already inserted: ${next.metaInfo}, trying to add: $element")
            }

            if (element.end <= next.metaInfo.start) {
                break
            }

            if (element.end < next.metaInfo.end) {
                error("Attempting to add an overlapping range. Already inserted: ${next.metaInfo}, trying to add: $element")
            }

            innerNodes[next.metaInfo.start] = next
            totalChildrenCount += next.totalChildrenCount
            iterator.remove()
        }

        val node = Node(element, innerNodes, totalChildrenCount)
        parent.children[element.start] = node

        for (it in parents) {
            it.totalChildrenCount++
        }

        return true
    }

    override fun clear() {
        root = NodeBase(TreeMap(), 0)
    }

    override fun toString() = "MetaInfoHierarchySet(${iterator().asSequence().joinToString()})"

    class IteratorStackElement(
        val node: NodeBase,
        val iterator: MutableIterator<MutableMap.MutableEntry<Int, Node>> = node.children.iterator(),
    )

    inner class Iterator : MutableIterator<ParsedCodeMetaInfo> {
        private val stack = mutableListOf(IteratorStackElement(root))

        override fun hasNext() = stack.asReversed().any { it.iterator.hasNext() }

        override fun next(): ParsedCodeMetaInfo {
            while (stack.isNotEmpty() && !stack.last().iterator.hasNext()) {
                stack.removeLast()
            }

            val stackTop = stack.lastOrNull() ?: throw NoSuchElementException()
            val next = stackTop.iterator.next()
            stack.add(IteratorStackElement(next.value))
            return next.value.metaInfo
        }

        override fun remove() {
            if (stack.size <= 1) {
                throw NoSuchElementException()
            }

            stack.removeLast()
            stack.lastOrNull()?.iterator?.remove()

            for (it in stack) {
                it.node.totalChildrenCount--
            }
        }
    }

    override fun iterator() = Iterator()
}

fun MetaInfoHierarchySet.NodeBase.findChildContaining(start: Int, end: Int) =
    children.floorEntry(start)?.value?.takeIf { end <= it.metaInfo.end }

fun MetaInfoHierarchySet.findParentsFor(start: Int, end: Int): List<MetaInfoHierarchySet.NodeBase> {
    val parents = mutableListOf(root)
    var current = parents.last().findChildContaining(start, end)

    while (current != null) {
        parents.add(current)
        current = current.findChildContaining(start, end)
    }

    return parents
}

fun MetaInfoHierarchySet.hasOverlappingEquivalentOf(element: ParsedCodeMetaInfo): Boolean {
    val parents = findParentsFor(element.start, element.end)
    val hasEquivalentParent = parents.any {
        it is MetaInfoHierarchySet.Node && it.metaInfo.equivalenceClass == element.equivalenceClass
    }

    if (hasEquivalentParent) {
        return true
    }

    return parents.last().hasOverlappingChildWhere(element) {
        it.metaInfo.equivalenceClass == element.equivalenceClass
    }
}

fun MetaInfoHierarchySet.overlapsWith(element: ParsedCodeMetaInfo): Boolean {
    val parents = findParentsFor(element.start, element.end)

    if (parents.last() is MetaInfoHierarchySet.Node) {
        return true
    }

    return parents.last().hasOverlappingChildWhere(element) { true }
}

private fun MetaInfoHierarchySet.NodeBase.hasOverlappingChildWhere(
    element: ParsedCodeMetaInfo,
    condition: (MetaInfoHierarchySet.Node) -> Boolean,
): Boolean {
    val iterator = children.iterator()

    while (iterator.hasNext()) {
        val next = iterator.next().value

        if (next.metaInfo.shiftedEnd <= element.shiftedStart) {
            continue
        }

        if (element.shiftedEnd <= next.metaInfo.shiftedStart) {
            break
        }

        if (condition(next) || next.hasOverlappingChildWhere(element, condition)) {
            return true
        }
    }

    return false
}

fun Collection<ParsedCodeMetaInfo>.toMetaInfosHierarchySet() =
    MetaInfoHierarchySet().also { it.addAll(this) }

@Suppress("unused")
fun MetaInfoHierarchySet.NodeBase.render(
    indent: String = "",
): String = buildString {
    if (this@render is MetaInfoHierarchySet.Node) {
        append(metaInfo.tag)
        append(" (")
        append(metaInfo.start)
        append(", ")
        append(metaInfo.end)
        append(")")
    } else {
        append("<root>")
    }

    val childRepresentations = children.map { (bound, node) ->
        "-- $bound: " + node.render("$indent--")
    }

    if (childRepresentations.isNotEmpty()) {
        append("\n")
        append(childRepresentations.joinToString("\n"))
    }
}
