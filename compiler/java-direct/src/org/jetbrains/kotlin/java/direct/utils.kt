/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder

import com.intellij.platform.syntax.parser.prepareProduction
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter

class JavaSyntaxNode(
    val type: SyntaxElementType,
    val children: List<JavaSyntaxNode>,
    val source: CharSequence,
    val startOffset: Int,
    val endOffset: Int,
    var parent: JavaSyntaxNode? = null
) {
    val text: String get() = source.subSequence(startOffset, endOffset).toString()

    fun dump(indent: String = ""): String {
        val sb = StringBuilder()
        sb.append(indent).append(type).append(": ").append(text.replace("\n", "\\n")).append("\n")
        for (child in children) {
            sb.append(child.dump(indent + "  "))
        }
        return sb.toString()
    }
}

fun buildSyntaxTree(builder: SyntaxTreeBuilder, source: CharSequence): JavaSyntaxNode {
    val productionMarkers = prepareProduction(builder).productionMarkers
    val tokens = builder.tokens
    val childrenStack = mutableListOf<MutableList<JavaSyntaxNode>>()
    childrenStack.add(mutableListOf())

    var prevTokenIndex = 0

    fun MutableList<JavaSyntaxNode>.appendTokens(lastTokenIndex: Int) {
        for (i in prevTokenIndex until lastTokenIndex) {
            val tokenType = tokens.getTokenType(i) ?: continue
            val start = tokens.getTokenStart(i)
            val end = tokens.getTokenEnd(i)
            if (start == end) continue
            add(JavaSyntaxNode(tokenType, emptyList(), source, start, end))
        }
        prevTokenIndex = lastTokenIndex
    }

    for (i in 0 until productionMarkers.size) {
        val production = productionMarkers.getMarker(i)
        when {
            productionMarkers.isDoneMarker(i) -> {
                val lastChildren = childrenStack.removeAt(childrenStack.size - 1)
                lastChildren.appendTokens(production.getEndTokenIndex())
                val node = JavaSyntaxNode(production.getNodeType(), lastChildren, source, production.getStartOffset(), production.getEndOffset())
                for (child in lastChildren) {
                    child.parent = node
                }
                childrenStack.last().add(node)
            }
            production.isErrorMarker() -> {
                // Error markers don't have start/done pairs, handle them as leaf nodes
                val errorTokenIndex = production.getStartTokenIndex()
                childrenStack.peek().appendTokens(errorTokenIndex)
                val errorNode = JavaSyntaxNode(production.getNodeType(), emptyList(), source, production.getStartOffset(), production.getEndOffset())
                childrenStack.peek().add(errorNode)
            }
            else -> {
                // Start marker
                childrenStack.peek().appendTokens(production.getStartTokenIndex())
                childrenStack.add(mutableListOf())
            }
        }
    }

    val rootNodeType = productionMarkers.getMarker(productionMarkers.size - 1).getNodeType()
    val rootChildren = childrenStack.single()
    val root = JavaSyntaxNode(rootNodeType, rootChildren, source, 0, source.length)
    for (child in rootChildren) {
        child.parent = root
    }
    return root
}

private fun <T> MutableList<T>.peek(): T = last()

fun JavaSyntaxNode.findChildByType(typeName: String): JavaSyntaxNode? {
    return children.find { it.type.toString() == typeName }
}

fun JavaSyntaxNode.getChildrenByType(typeName: String): List<JavaSyntaxNode> {
    return children.filter { it.type.toString() == typeName }
}

fun JavaSyntaxNode.findChildByType(type: SyntaxElementType): JavaSyntaxNode? {
    return children.find { it.type == type }
}

fun JavaSyntaxNode.getChildrenByType(type: SyntaxElementType): List<JavaSyntaxNode> {
    return children.filter { it.type == type }
}

internal fun computeTypeParameters(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
): List<JavaTypeParameter> {
    val typeParamNodes = node.findChildByType("TYPE_PARAMETER_LIST")
        ?.getChildrenByType("TYPE_PARAMETER")
        ?: return emptyList()

    // Create type parameter instances first
    val typeParams = typeParamNodes.map { JavaTypeParameterOverAst(it, resolutionContext) }

    // Create a resolution context with ALL type parameters in scope.
    // This is needed for resolving bounds like `<E, S extends List<E>>`.
    val contextWithTypeParams = resolutionContext.withTypeParameters(typeParams)

    // Update each type parameter to use the enriched context for bounds resolution
    typeParams.forEach { it.updateResolutionContext(contextWithTypeParams) }

    return typeParams
}

