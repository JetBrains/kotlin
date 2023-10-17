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
    private data class InlineScopeInfo(
        val variable: LocalVariableNode,
        val scopeNumber: Int,
        val ivDepth: Int,
        val oldScopeNumber: Int
    )

    var inlinedScopes = 0
    var currentCallSiteLineNumber = 0

    fun addInlineScopesInfo(node: MethodNode) {
        val localVariables = node.localVariables ?: return
        val labelToIndex = node.getLabelToIndexMap()

        fun LocalVariableNode.contains(other: LocalVariableNode): Boolean {
            val startIndex = labelToIndex[start.label] ?: return false
            val endIndex = labelToIndex[end.label] ?: return false
            val otherStartIndex = labelToIndex[other.start.label] ?: return false
            val otherEndIndex = labelToIndex[other.end.label] ?: return false
            return startIndex < otherStartIndex && endIndex >= otherEndIndex
        }

        // The scope number 0 belongs to the top frame
        var currentInlineScopeNumber = 0

        // Inline function and lambda parameters are introduced before the corresponding inline marker variable,
        // so we need to keep track of them to assign the correct scope number later.
        val variablesWithNotMatchingDepth = mutableListOf<LocalVariableNode>()

        // This list is used to keep track of active inline scopes and to map
        // from a number of $iv suffixes to correct scope number.
        val inlineScopesStack = mutableListOf<InlineScopeInfo>()
        var seenInlineScopesNumber = 0
        var oldScopeNumberOfCurrentMarkerVariable = -1

        val labelToLineNumber = node.getLabelToLineNumberMap()
        // The depth in $iv suffixes
        var currentIVDepth = -1
        val sortedVariables = localVariables.sortedBy { labelToIndex[it.start.label] }
        val ignoreScopeNumbers = sortedVariables.any {
            it.name.isInlineFunctionMarkerVariableName && !it.name.contains(INLINE_SCOPE_NUMBER_SEPARATOR)
        }
        for (variable in sortedVariables) {
            while (inlineScopesStack.isNotEmpty() && !inlineScopesStack.last().variable.contains(variable)) {
                inlineScopesStack.removeLast()
            }

            if (inlineScopesStack.isNotEmpty()) {
                val lastScope = inlineScopesStack.last()
                currentInlineScopeNumber = lastScope.scopeNumber
                currentIVDepth = lastScope.ivDepth
                oldScopeNumberOfCurrentMarkerVariable = lastScope.oldScopeNumber
            }

            val name = variable.name
            val nameBuilder = VariableNameBuilder(name)
            val scopeNumber = nameBuilder.scopeNumber
            when {
                isFakeLocalVariableForInline(name) -> {
                    seenInlineScopesNumber += 1
                    oldScopeNumberOfCurrentMarkerVariable = scopeNumber ?: -1
                    currentInlineScopeNumber = seenInlineScopesNumber

                    currentIVDepth =
                        if (name.isInlineLambdaMarkerVariableName) {
                            getInlineDepth(name)
                        } else {
                            currentIVDepth + 1
                        }

                    val callSiteLineNumber =
                        if (currentInlineScopeNumber == 1) {
                            currentCallSiteLineNumber
                        } else {
                            nameBuilder.callSiteLineNumber?.takeIf { !ignoreScopeNumbers } ?:
                            // When inlining from the code compiled by the old compiler versions,
                            // the marker variable will not contain the call site line number.
                            // In this case we will take the line number of the variable start offset
                            // as the call site line number.
                            labelToLineNumber[variable.start.label] ?: 0
                        }

                    nameBuilder.scopeNumber = currentInlineScopeNumber + inlinedScopes
                    nameBuilder.callSiteLineNumber = callSiteLineNumber
                    if (name.isInlineLambdaMarkerVariableName) {
                        val surroundingScopeNumber = nameBuilder.surroundingScopeNumber
                        val newSurroundingScopeNumber = when {
                            // The first encountered inline scope belongs to the lambda, which means
                            // that its surrounding scope is the function where the lambda is being inlined to.
                            currentInlineScopeNumber == 1 -> 0
                            // Every lambda that is already inlined must have a surrounding scope number.
                            // If it doesn't, then it means that we are inlining the code compiled by
                            // the older versions of the Kotlin compiler, where surrounding scope numbers
                            // haven't been introduced yet.
                            surroundingScopeNumber != null && !ignoreScopeNumbers -> surroundingScopeNumber + inlinedScopes + 1
                            // If a lambda doesn't have a surrounding scope number, we will calculate its
                            // depth using the number of the $iv suffixes
                            else -> {
                                val surroundingScopeInfo =
                                    if (currentIVDepth != 0) {
                                        inlineScopesStack.asReversed().firstOrNull {
                                            it.ivDepth == currentIVDepth
                                        }
                                    } else {
                                        inlineScopesStack.asReversed().firstOrNull {
                                            it.variable.name.isInlineLambdaMarkerVariableName
                                        }
                                    } ?: inlineScopesStack.firstOrNull()
                                surroundingScopeInfo?.scopeNumber?.plus(inlinedScopes) ?: 0
                            }
                        }
                        nameBuilder.surroundingScopeNumber = newSurroundingScopeNumber
                    }
                    variable.name = nameBuilder.build()

                    inlineScopesStack += InlineScopeInfo(
                        variable,
                        currentInlineScopeNumber,
                        currentIVDepth,
                        oldScopeNumberOfCurrentMarkerVariable
                    )

                    variablesWithNotMatchingDepth.forEach {
                        it.name = it.name.addScopeNumber(currentInlineScopeNumber + inlinedScopes)
                    }
                    variablesWithNotMatchingDepth.clear()
                }
                scopeNumber != null && !ignoreScopeNumbers -> {
                    if (scopeNumber != oldScopeNumberOfCurrentMarkerVariable) {
                        variablesWithNotMatchingDepth.add(variable)
                    } else {
                        variable.name = name.addScopeNumber(currentInlineScopeNumber + inlinedScopes)
                    }
                }
                else -> {
                    if (inlineScopesStack.size == 0 || getInlineDepth(name) != currentIVDepth) {
                        variablesWithNotMatchingDepth.add(variable)
                    } else {
                        variable.name = name.addScopeNumber(currentInlineScopeNumber + inlinedScopes)
                    }
                }
            }
        }

        inlinedScopes += seenInlineScopesNumber
    }
}

internal class VariableNameBuilder(name: String) {
    var scopeNumber: Int?
    var callSiteLineNumber: Int?
    var surroundingScopeNumber: Int?
    private val prefix: String
    private val ivDepth: Int

    init {
        val info = name.getInlineScopeInfo()
        scopeNumber = info?.scopeNumber
        callSiteLineNumber = info?.callSiteLineNumber
        surroundingScopeNumber = info?.surroundingScopeNumber
        ivDepth = getInlineDepth(name)
        prefix = name.replace(INLINE_FUN_VAR_SUFFIX, "").dropInlineScopeInfo()
    }

    fun build(): String = buildString {
        append(prefix)
        if (scopeNumber != null) {
            append(INLINE_SCOPE_NUMBER_SEPARATOR)
            append(scopeNumber ?: 0)
        }

        if (callSiteLineNumber != null) {
            append(INLINE_SCOPE_NUMBER_SEPARATOR)
            append(callSiteLineNumber)
        }

        if (surroundingScopeNumber != null) {
            append(INLINE_SCOPE_NUMBER_SEPARATOR)
            append(surroundingScopeNumber)
        }

        repeat(ivDepth) {
            append(INLINE_FUN_VAR_SUFFIX)
        }
    }
}

fun String.addScopeNumber(scopeNumber: Int) =
    VariableNameBuilder(this).apply { this.scopeNumber = scopeNumber }.build()

fun updateCallSiteLineNumber(name: String, lineNumberMapping: Map<Int, Int>): String =
    updateCallSiteLineNumber(name) { lineNumberMapping[it] ?: it }

fun updateCallSiteLineNumber(name: String, newLineNumber: Int): String =
    updateCallSiteLineNumber(name) { newLineNumber }

private fun updateCallSiteLineNumber(name: String, calculate: (Int) -> Int): String {
    val nameBuilder = VariableNameBuilder(name)
    val callSiteLineNumber = nameBuilder.callSiteLineNumber
    if (nameBuilder.scopeNumber == null || callSiteLineNumber == null) {
        return name
    }

    val newLineNumber = calculate(callSiteLineNumber)
    if (newLineNumber == callSiteLineNumber) {
        return name
    }
    nameBuilder.callSiteLineNumber = newLineNumber
    return nameBuilder.build()
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

private val String.isInlineLambdaMarkerVariableName: Boolean
    get() = startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)

private val String.isInlineFunctionMarkerVariableName: Boolean
    get() = startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION)
