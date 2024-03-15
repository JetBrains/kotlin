/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.LineNumberNode
import org.jetbrains.org.objectweb.asm.tree.LocalVariableNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class InlineScopesGenerator {
    var inlinedScopes = 0
    var currentCallSiteLineNumber = 0

    private class InlineScopeNode(
        // The marker variable is only null for the root node
        val markerVariable: LocalVariableNode?,
        val scopeNumber: Int,
        var inlineNesting: Int,
        val parent: InlineScopeNode?
    ) {
        var callSiteLineNumber: Int? = null
        var surroundingScopeNumber: Int? = null

        val variables = mutableListOf<LocalVariableNode>()
        val children = mutableListOf<InlineScopeNode>()

        val isRoot: Boolean
            get() = parent == null

        init {
            parent?.children?.add(this)
        }
    }

    private abstract inner class VariableRenamer {
        abstract fun computeInlineScopeInfo(node: InlineScopeNode)

        abstract fun LocalVariableNode.belongsToInlineScope(node: InlineScopeNode): Boolean

        open fun shouldSkipVariable(variable: LocalVariableNode): Boolean = false

        open fun inlineNesting(): Int = -1

        fun renameVariables(methodNode: MethodNode): Int {
            val rootNode = computeInlineScopesTree(methodNode)
            return renameVariables(rootNode)
        }

        private fun computeInlineScopesTree(methodNode: MethodNode): InlineScopeNode {
            val rootNode = InlineScopeNode(null, 0, inlineNesting(), null)
            val localVariables = methodNode.localVariables ?: return rootNode

            // Inline function and lambda parameters are introduced before the corresponding inline marker variable,
            // so we need to keep track of them to assign to the correct scope later.
            val variablesWithNotMatchingDepth = mutableListOf<LocalVariableNode>()

            val labelToIndex = methodNode.getLabelToIndexMap()
            val sortedVariables = localVariables.sortedBy { labelToIndex[it.start.label] }

            var seenInlineScopesNumber = 0
            var currentNode = rootNode
            for (variable in sortedVariables) {
                currentNode = currentNode.findClosestSurroundingScope(variable, labelToIndex)

                val name = variable.name
                if (isFakeLocalVariableForInline(name)) {
                    seenInlineScopesNumber += 1

                    val newNode = InlineScopeNode(variable, seenInlineScopesNumber, currentNode.inlineNesting, currentNode)
                    computeInlineScopeInfo(newNode)
                    currentNode = newNode

                    currentNode.variables.addAll(variablesWithNotMatchingDepth)
                    variablesWithNotMatchingDepth.clear()
                } else if (!currentNode.isRoot || !shouldSkipVariable(variable)) {
                    if (variable.belongsToInlineScope(currentNode)) {
                        currentNode.variables.add(variable)
                    } else {
                        variablesWithNotMatchingDepth.add(variable)
                    }
                }
            }

            return rootNode
        }

        private fun renameVariables(rootNode: InlineScopeNode): Int {
            var seenInlineScopesNumber = 0
            val nodeStack = mutableListOf<InlineScopeNode>()
            nodeStack.addAll(rootNode.children)
            while (nodeStack.isNotEmpty()) {
                val node = nodeStack.removeLast()
                seenInlineScopesNumber += 1
                with(node) {
                    markerVariable!!.name = computeNewVariableName(
                        markerVariable.name,
                        scopeNumber + inlinedScopes,
                        callSiteLineNumber,
                        surroundingScopeNumber
                    )
                }

                for (variable in node.variables) {
                    variable.name = computeNewVariableName(
                        variable.name,
                        node.scopeNumber + inlinedScopes,
                        null,
                        null
                    )
                }

                nodeStack.addAll(node.children)
            }

            return seenInlineScopesNumber
        }

        private fun InlineScopeNode.findClosestSurroundingScope(
            variable: LocalVariableNode,
            labelToIndex: Map<Label, Int>
        ): InlineScopeNode {
            fun LocalVariableNode.contains(other: LocalVariableNode): Boolean {
                val startIndex = labelToIndex[start.label] ?: return false
                val endIndex = labelToIndex[end.label] ?: return false
                val otherStartIndex = labelToIndex[other.start.label] ?: return false
                val otherEndIndex = labelToIndex[other.end.label] ?: return false
                return startIndex < otherStartIndex && endIndex >= otherEndIndex
            }

            var node = this
            while (!node.isRoot && !node.markerVariable!!.contains(variable)) {
                node = node.parent!!
            }
            return node
        }
    }

    fun addInlineScopesInfo(node: MethodNode, isRegeneratingAnonymousObject: Boolean) {
        val localVariables = node.localVariables
        if (localVariables?.isEmpty() == true) {
            return
        }

        val markerVariablesWithoutScopeInfoNum = localVariables.count {
            isFakeLocalVariableForInline(it.name) && !it.name.contains(INLINE_SCOPE_NUMBER_SEPARATOR)
        }

        when {
            isRegeneratingAnonymousObject -> {
                if (markerVariablesWithoutScopeInfoNum > 0) {
                    addInlineScopesInfoFromIVSuffixesWhenRegeneratingAnonymousObject(node)
                }
            }
            // When inlining a function its marker variable won't contain any scope numbers yet.
            // But if there are more than one marker variable like this, it means that we
            // are inlining the code produced by the old compiler versions, where inline scopes
            // have not been introduced.
            markerVariablesWithoutScopeInfoNum == 1 ->
                addInlineScopesInfoFromScopeNumbers(node)
            else ->
                addInlineScopesInfoFromIVSuffixes(node)
        }
    }

    private fun addInlineScopesInfoFromScopeNumbers(node: MethodNode) {
        val renamer = object : VariableRenamer() {
            override fun computeInlineScopeInfo(node: InlineScopeNode) {
                val name = node.markerVariable!!.name
                val scopeNumber = node.scopeNumber
                val info = name.getInlineScopeInfo()
                node.inlineNesting = info?.scopeNumber ?: 0
                node.callSiteLineNumber =
                    if (scopeNumber == 1) {
                        currentCallSiteLineNumber
                    } else {
                        info?.callSiteLineNumber ?: 0
                    }

                if (name.isInlineLambdaName) {
                    val surroundingScopeNumber = info?.surroundingScopeNumber
                    node.surroundingScopeNumber =
                        when {
                            // The first encountered inline scope belongs to the lambda, which means
                            // that its surrounding scope is the function where the lambda is being inlined to.
                            scopeNumber == 1 -> 0
                            // Every lambda that is already inlined must have a surrounding scope number.
                            // If it doesn't, then it means that we are inlining the code compiled by
                            // the older versions of the Kotlin compiler, where surrounding scope numbers
                            // haven't been introduced yet.
                            surroundingScopeNumber != null -> surroundingScopeNumber + inlinedScopes + 1
                            // This situation shouldn't happen, so add invalid info here
                            else -> -1
                        }
                }
            }

            override fun LocalVariableNode.belongsToInlineScope(node: InlineScopeNode): Boolean {
                val scopeNumber = name.getInlineScopeInfo()?.scopeNumber
                val oldScopeNumberOfCurrentMarkerVariable = node.inlineNesting
                if (scopeNumber != null) {
                    return scopeNumber == oldScopeNumberOfCurrentMarkerVariable
                }
                return !node.isRoot
            }
        }

        inlinedScopes += renamer.renameVariables(node)
    }

    private fun addInlineScopesInfoFromIVSuffixes(node: MethodNode) {
        val labelToLineNumber = node.getLabelToLineNumberMap()

        val renamer = object : VariableRenamer() {
            override fun computeInlineScopeInfo(node: InlineScopeNode) {
                val variable = node.markerVariable!!
                val name = variable.name
                val ivDepth = node.inlineNesting
                val scopeNumber = node.scopeNumber
                node.inlineNesting =
                    if (name.isInlineLambdaName) {
                        getInlineDepth(name)
                    } else {
                        ivDepth + 1
                    }

                node.callSiteLineNumber =
                    if (scopeNumber == 1) {
                        currentCallSiteLineNumber
                    } else {
                        // When inlining from the code compiled by the old compiler versions,
                        // the marker variable will not contain the call site line number.
                        // In this case we will take the line number of the variable start offset
                        // as the call site line number.
                        labelToLineNumber[variable.start.label] ?: 0
                    }

                if (name.isInlineLambdaName) {
                    node.surroundingScopeNumber = computeSurroundingScopeNumber(node)
                }
            }

            override fun LocalVariableNode.belongsToInlineScope(node: InlineScopeNode): Boolean =
                !node.isRoot && getInlineDepth(name) == node.inlineNesting
        }

        inlinedScopes += renamer.renameVariables(node)
    }

    private fun addInlineScopesInfoFromIVSuffixesWhenRegeneratingAnonymousObject(node: MethodNode) {
        val labelToLineNumber = node.getLabelToLineNumberMap()

        // This renamer is slightly different from the one we used when computing inline scopes from the
        // $iv suffixes. Here no function is being inlined, so the base depth in $iv suffixes is equal to 0.
        // When we meet the first marker variable, it should have its depth equal to 1. Apart from that,
        // when calculating call site line numbers, we always pick the line number of the marker variable
        // start offset and not rely on the `currentCallSiteLineNumber` field. Also, when computing surrounding
        // scope numbers we assign surrounding scope 0 (that represents the top frame) to inline lambda
        // marker variables that don't have a surrounding scope.
        val renamer = object : VariableRenamer() {
            // Here inline nesting means depth in $iv suffixes.
            // On contrary with the situation when we are inlining a function,
            // here we won't meet a marker variable that represents the method node.
            // When we meet the first marker variable, it should have depth equal to 1.
            override fun inlineNesting(): Int = 0

            override fun shouldSkipVariable(variable: LocalVariableNode): Boolean =
                !variable.name.contains(INLINE_FUN_VAR_SUFFIX)

            override fun computeInlineScopeInfo(node: InlineScopeNode) {
                val variable = node.markerVariable!!
                val ivDepth = node.inlineNesting
                val name = variable.name
                node.inlineNesting =
                    if (name.isInlineLambdaName) {
                        getInlineDepth(name)
                    } else {
                        ivDepth + 1
                    }

                node.callSiteLineNumber = labelToLineNumber[variable.start.label] ?: 0
                if (name.isInlineLambdaName) {
                    node.surroundingScopeNumber = computeSurroundingScopeNumber(node)
                }
            }

            override fun LocalVariableNode.belongsToInlineScope(node: InlineScopeNode): Boolean =
                !node.isRoot && getInlineDepth(name) == node.inlineNesting
        }

        renamer.renameVariables(node)
    }

    private fun computeSurroundingScopeNumber(currentNode: InlineScopeNode): Int {
        val scopeNumber = currentNode.scopeNumber
        val currentIVDepth = currentNode.inlineNesting
        if (scopeNumber == 1) {
            return 0
        }

        var surroundingScopeNumber: Int? = null
        var node = currentNode.parent
        while (node != null && !node.isRoot) {
            if (node.inlineNesting == currentIVDepth) {
                surroundingScopeNumber = node.scopeNumber
                break
            }
            node = node.parent
        }

        return surroundingScopeNumber?.plus(inlinedScopes) ?: 0
    }

    private fun computeNewVariableName(
        name: String,
        scopeNumber: Int,
        callSiteLineNumber: Int?,
        surroundingScopeNumber: Int?
    ): String {
        val prefix = name.replace(INLINE_FUN_VAR_SUFFIX, "").dropInlineScopeInfo()
        return buildString {
            append(prefix)
            append(INLINE_SCOPE_NUMBER_SEPARATOR)
            append(scopeNumber)

            if (callSiteLineNumber != null) {
                append(INLINE_SCOPE_NUMBER_SEPARATOR)
                append(callSiteLineNumber)

                if (surroundingScopeNumber != null) {
                    append(INLINE_SCOPE_NUMBER_SEPARATOR)
                    append(surroundingScopeNumber)
                }
            }
        }
    }
}

fun updateCallSiteLineNumber(name: String, newLineNumber: Int): String =
    updateCallSiteLineNumber(name) { newLineNumber }

fun updateCallSiteLineNumber(name: String, calculate: (Int) -> Int): String {
    val (scopeNumber, callSiteLineNumber, surroundingScopeNumber) = name.getInlineScopeInfo() ?: return name
    if (callSiteLineNumber == null) {
        return name
    }

    val newLineNumber = calculate(callSiteLineNumber)
    if (newLineNumber == callSiteLineNumber) {
        return name
    }

    val newName = name
        .dropInlineScopeInfo()
        .addScopeInfo(scopeNumber)
        .addScopeInfo(newLineNumber)

    if (surroundingScopeNumber == null) {
        return newName
    }
    return newName.addScopeInfo(surroundingScopeNumber)
}

internal fun MethodNode.getLabelToIndexMap(): Map<Label, Int> =
    instructions.filterIsInstance<LabelNode>()
        .withIndex()
        .associate { (index, labelNode) ->
            labelNode.label to index
        }

private fun MethodNode.getLabelToLineNumberMap(): Map<Label, Int> {
    val result = mutableMapOf<Label, Int>()
    var currentLineNumber = 0
    for (insn in instructions) {
        if (insn is LineNumberNode) {
            currentLineNumber = insn.line
        } else if (insn is LabelNode) {
            result[insn.label] = currentLineNumber
        }
    }

    return result
}

fun String.addScopeInfo(number: Int): String =
    "$this$INLINE_SCOPE_NUMBER_SEPARATOR$number"

private fun getInlineDepth(variableName: String): Int {
    var endIndex = variableName.length
    var depth = 0

    val suffixLen = INLINE_FUN_VAR_SUFFIX.length
    while (endIndex >= suffixLen) {
        if (variableName.substring(endIndex - suffixLen, endIndex) != INLINE_FUN_VAR_SUFFIX) {
            break
        }

        depth++
        endIndex -= suffixLen
    }

    return depth
}

private val String.isInlineLambdaName: Boolean
    get() = startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)
