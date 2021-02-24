/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NonNls
import kotlin.collections.ArrayList

abstract class NodeIndentStrategy {

    abstract fun getIndent(node: ASTNode, settings: CodeStyleSettings): Indent?

    class ConstIndentStrategy(private val indent: Indent) : NodeIndentStrategy() {

        override fun getIndent(node: ASTNode, settings: CodeStyleSettings): Indent? {
            return indent
        }
    }

    class PositionStrategy(private val debugInfo: String?) : NodeIndentStrategy() {
        private var indentCallback: (CodeStyleSettings) -> Indent = { Indent.getNoneIndent() }

        private val within = ArrayList<IElementType>()
        private var withinCallback: ((ASTNode) -> Boolean)? = null

        private val notIn = ArrayList<IElementType>()

        private val forElement = ArrayList<IElementType>()
        private val notForElement = ArrayList<IElementType>()
        private var forElementCallback: ((ASTNode) -> Boolean)? = null

        override fun toString(): String {
            return "PositionStrategy " + (debugInfo ?: "No debug info")
        }

        fun set(indent: Indent): PositionStrategy {
            indentCallback = { indent }
            return this
        }

        fun set(indentCallback: (CodeStyleSettings) -> Indent): PositionStrategy {
            this.indentCallback = indentCallback
            return this
        }

        fun within(parents: TokenSet): PositionStrategy {
            val types = parents.types
            if (types.isEmpty()) {
                throw IllegalArgumentException("Empty token set is unexpected")
            }

            fillTypes(within, types[0], types.copyOfRange(1, types.size))
            return this
        }

        fun within(parentType: IElementType, vararg orParentTypes: IElementType): PositionStrategy {
            fillTypes(within, parentType, orParentTypes)
            return this
        }

        fun within(callback: (ASTNode) -> Boolean): PositionStrategy {
            withinCallback = callback
            return this
        }

        fun notWithin(parentType: IElementType, vararg orParentTypes: IElementType): PositionStrategy {
            fillTypes(notIn, parentType, orParentTypes)
            return this
        }

        fun withinAny(): PositionStrategy {
            within.clear()
            notIn.clear()
            return this
        }

        fun forType(elementType: IElementType, vararg otherTypes: IElementType): PositionStrategy {
            fillTypes(forElement, elementType, otherTypes)
            return this
        }

        fun notForType(elementType: IElementType, vararg otherTypes: IElementType): PositionStrategy {
            fillTypes(notForElement, elementType, otherTypes)
            return this
        }

        fun forAny(): PositionStrategy {
            forElement.clear()
            notForElement.clear()
            return this
        }

        fun forElement(callback: (ASTNode) -> Boolean): PositionStrategy {
            forElementCallback = callback
            return this
        }

        override fun getIndent(node: ASTNode, settings: CodeStyleSettings): Indent? {
            if (!isValidIndent(forElement, notForElement, node, forElementCallback)) return null

            val parent = node.treeParent
            if (parent != null) {
                if (!isValidIndent(within, notIn, parent, withinCallback)) return null
            } else if (within.isNotEmpty()) return null

            return indentCallback(settings)
        }

        private fun fillTypes(resultCollection: MutableList<IElementType>, singleType: IElementType, otherTypes: Array<out IElementType>) {
            resultCollection.clear()
            resultCollection.add(singleType)
            resultCollection.addAll(otherTypes)
        }
    }

    companion object {
        fun constIndent(indent: Indent): NodeIndentStrategy {
            return ConstIndentStrategy(indent)
        }

        fun strategy(@NonNls debugInfo: String?): PositionStrategy {
            return PositionStrategy(debugInfo)
        }
    }
}

private fun isValidIndent(
    elements: ArrayList<IElementType>,
    excludeElements: ArrayList<IElementType>,
    node: ASTNode,
    callback: ((ASTNode) -> Boolean)?
): Boolean {
    if (elements.isNotEmpty() && !elements.contains(node.elementType)) return false
    if (excludeElements.contains(node.elementType)) return false
    if (callback?.invoke(node) == false) return false
    return true
}