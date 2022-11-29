/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.IrWhenUtils
import org.jetbrains.kotlin.backend.wasm.WasmSymbols
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isElseBranch
import org.jetbrains.kotlin.wasm.ir.*

private class ExtractedWhenCondition<T>(val condition: IrCall, val const: IrConst<T>)
private class ExtractedWhenBranch<T>(val conditions: List<ExtractedWhenCondition<T>>, val expression: IrExpression)

internal fun BodyGenerator.tryGenerateOptimisedWhen(expression: IrWhen, symbols: WasmSymbols): Boolean {
    if (expression.branches.size <= 2) return false

    var elseExpression: IrExpression? = null
    val extractedBranches = mutableListOf<ExtractedWhenBranch<Any>>()

    // Parse when structure. Note that the condition can be nested. See matchConditions() for details.
    var noMultiplyConditionBranches = true
    val seenConditions = mutableSetOf<Any>() //to filter out equal conditions
    for (branch in expression.branches) {
        if (isElseBranch(branch)) {
            elseExpression = branch.result
        } else {
            val conditions = IrWhenUtils.matchConditions(symbols.irBuiltIns.ororSymbol, branch.condition) ?: return false
            val extractedConditions = tryExtractEqEqNumberConditions(symbols, conditions) ?: return false
            val filteredExtractedConditions = extractedConditions.filter { it.const.value !in seenConditions }
            seenConditions.addAll(extractedConditions.map { it.const.value })
            if (filteredExtractedConditions.isNotEmpty()) {
                noMultiplyConditionBranches = noMultiplyConditionBranches && filteredExtractedConditions.size == 1
                extractedBranches.add(ExtractedWhenBranch(filteredExtractedConditions, branch.result))
            }
        }
    }
    if (extractedBranches.isEmpty()) return false
    val subject = extractedBranches[0].conditions[0].condition.getValueArgument(0) ?: return false

    // Check all kinds are the same
    for (branch in extractedBranches) {
        //TODO: Support all primitive types
        if (!branch.conditions.all { it.const.kind.equals(IrConstKind.Int) }) return false
    }

    val intBranches = extractedBranches.map { branch ->
        @Suppress("UNCHECKED_CAST")
        branch as ExtractedWhenBranch<Int>
    }

    val maxValue = intBranches.maxOf { branch -> branch.conditions.maxOf { it.const.value } }
    val minValue = intBranches.minOf { branch -> branch.conditions.minOf { it.const.value } }
    if (minValue == maxValue) return false

    val selectorLocal = context.referenceLocal(SyntheticLocalType.TABLE_SWITCH_SELECTOR)
    generateExpression(subject)
    body.buildSetLocal(selectorLocal)

    val resultType = context.transformBlockResultType(expression.type)
    //int overflow or load is too small then make table switch
    val tableSize = maxValue - minValue
    if (tableSize <= 0 || tableSize > seenConditions.size * 2) {
        if (noMultiplyConditionBranches) {
            createBinaryTable(
                selectorLocal = selectorLocal,
                intBranches = intBranches,
                elseExpression = elseExpression,
                resultType = resultType,
                expectedType = expression.type,
            )
        } else {
            createBinaryTable(selectorLocal, intBranches)
            body.buildSetLocal(selectorLocal)
            genTableIntSwitch(
                selectorLocal = selectorLocal,
                resultType = resultType,
                branches = intBranches,
                elseExpression = elseExpression,
                shift = 0,
                brTable = intBranches.indices.toList(),
                expectedType = expression.type,
            )
        }
    } else {
        val brTable = mutableListOf<Int>()
        for (i in minValue..maxValue) {
            val branchIndex = intBranches.indexOfFirst { branch -> branch.conditions.any { it.const.value == i } }
            val brIndex = if (branchIndex != -1) branchIndex else intBranches.size
            brTable.add(brIndex)
        }
        genTableIntSwitch(
            selectorLocal = selectorLocal,
            resultType = resultType,
            branches = intBranches,
            elseExpression = elseExpression,
            shift = minValue,
            brTable = brTable,
            expectedType = expression.type,
        )
    }
    return true
}

/**
 * Create binary search for when that emit branches index in leafs
 * when (a) {
 *   123 -> expr1
 *   456 -> expr2
 *   else -> elseExpr
 * }
 * crates binary search linked to index of branch
 *   IF (a < 456) {
 *      IF (a == 123)
 *        #expr1
 *      ELSE
 *        #else
 *      END IF
 *   ELSE
 *      IF (1 == 456)
 *        #expr2
 *      ELSE
 *        #else
 *      END IF
 *   END IF
 * }
 */
private fun BodyGenerator.createBinaryTable(selectorLocal: WasmLocal, intBranches: List<ExtractedWhenBranch<Int>>) {
    val sortedCaseToBranchIndex = mutableListOf<Pair<Int, Int>>()
    intBranches.flatMapIndexedTo(sortedCaseToBranchIndex) { index, branch -> branch.conditions.map { it.const.value to index } }
    sortedCaseToBranchIndex.sortBy { it.first }

    val thenBody = { result: Int ->
        body.buildConstI32(result)
    }
    val elseBody: () -> Unit = {
        body.buildConstI32(intBranches.size)
    }
    createBinaryTable(selectorLocal, WasmI32, sortedCaseToBranchIndex, 0, sortedCaseToBranchIndex.size, thenBody, elseBody)
}

private fun tryExtractEqEqNumberConditions(symbols: WasmSymbols, conditions: List<IrCall>): List<ExtractedWhenCondition<Any>>? {
    if (conditions.isEmpty()) return null

    val firstCondition = conditions[0]
    val firstConditionSymbol = firstCondition.symbol
        .takeIf { it in symbols.equalityFunctions.values }
        ?: return null
    if (firstCondition.valueArgumentsCount != 2) return null

    // All conditions has the same eqeq
    if (conditions.any { it.symbol != firstConditionSymbol }) return null

    val result = mutableListOf<ExtractedWhenCondition<Any>>()
    for (condition in conditions) {
        if (condition.symbol != firstConditionSymbol) return null
        @Suppress("UNCHECKED_CAST")
        val conditionConst = condition.getValueArgument(1) as? IrConst<Any> ?: return null
        result.add(ExtractedWhenCondition(condition, conditionConst))
    }

    return result
}

/**
 * Create binary search for when that emit when expressions in leafs
 * when (a) {
 *   123 -> expr1
 *   456 -> expr2
 *   else -> elseExpr
 * }
 * crates binary search linked to index of branch
 * BLOCK
 *   IF (a < 456) {
 *      IF (a == 123)
 *        #expr1
 *        GOTO END BLOCK
 *      END IF
 *   ELSE
 *      IF (1 == 456)
 *        #expr2
 *        GOTO END BLOCK
 *      END IF
 *   END IF
 *   elseExpr
 * END BLOCK
 * }
*/
private fun BodyGenerator.createBinaryTable(
    selectorLocal: WasmLocal,
    intBranches: List<ExtractedWhenBranch<Int>>,
    elseExpression: IrExpression?,
    resultType: WasmType?,
    expectedType: IrType,
) {
    val sortedCaseToBranchIndex = mutableListOf<Pair<Int, IrExpression>>()
    intBranches.mapTo(sortedCaseToBranchIndex) { branch -> branch.conditions[0].const.value to branch.expression }
    sortedCaseToBranchIndex.sortBy { it.first }

    body.buildBlock("when_block", resultType) { currentBlock ->
        val thenBody = { result: IrExpression ->
            generateWithExpectedType(result, expectedType)
            body.buildBr(currentBlock)
        }
        createBinaryTable(
            selectorLocal = selectorLocal,
            resultType = null,
            sortedCases = sortedCaseToBranchIndex,
            fromIncl = 0,
            toExcl = sortedCaseToBranchIndex.size,
            thenBody = thenBody,
            elseBody = { }
        )

        if (elseExpression != null) {
            generateWithExpectedType(elseExpression, expectedType)
        } else {
            // default else block
            if (resultType != null) {
                if (expectedType.isUnit()) {
                    body.buildGetUnit()
                } else {
                    body.buildUnreachable()
                }
            }
        }
    }
}

private fun <T> BodyGenerator.createBinaryTable(
    selectorLocal: WasmLocal,
    resultType: WasmType?,
    sortedCases: List<Pair<Int, T>>,
    fromIncl: Int,
    toExcl: Int,
    thenBody: (T) -> Unit,
    elseBody: () -> Unit
) {
    val size = toExcl - fromIncl
    if (size == 1) {
        val (case, result) = sortedCases[fromIncl]
        body.buildGetLocal(selectorLocal)
        body.buildConstI32(case)
        body.buildInstr(WasmOp.I32_EQ)
        body.buildIf("binary_tree_branch", resultType)
        thenBody(result)
        body.buildElse()
        elseBody()
        body.buildEnd()
        return
    }

    val border = fromIncl + size / 2

    body.buildGetLocal(selectorLocal)
    body.buildConstI32(sortedCases[border].first)
    body.buildInstr(WasmOp.I32_LT_S)
    body.buildIf("binary_tree_node", resultType)
    createBinaryTable(selectorLocal, resultType, sortedCases, fromIncl, border, thenBody, elseBody)
    body.buildElse()
    createBinaryTable(selectorLocal, resultType, sortedCases, border, toExcl, thenBody, elseBody)
    body.buildEnd()
}


/**
 * Create table switch with expressions
 * when (a) {
 *   0 -> expr1
 *   1 -> expr2
 *   else -> elseExpr
 * }
 * crates binary search linked to index of branch
 * BLOCK FOR ELSE
 *   BLOCK1
 *     BLOCK2
 *       BLOCK FOR BRTABLE
 *         BRTABLE 0 1 2
 *       END BLOCK FOR BRTABLE
 *       expr1
 *       GOTO END BLOCK FOR ELSE
 *     END BLOCK1
 *     expr2
 *     GOTO END BLOCK FOR ELSE
 *   END BLOCK2
 *   elseExpr
 * END BLOCK FOR ELSE
 */
private fun BodyGenerator.genTableIntSwitch(
    selectorLocal: WasmLocal,
    resultType: WasmType?,
    branches: List<ExtractedWhenBranch<Int>>,
    elseExpression: IrExpression?,
    shift: Int,
    brTable: List<Int>,
    expectedType: IrType,
) {
    val baseBlockIndex = body.numberOfNestedBlocks
    //expressions + else branch + br_table
    repeat(branches.size + 2) {
        body.buildBlock(resultType)
    }

    resultType?.let { generateDefaultInitializerForType(it, body) } //stub value
    body.buildGetLocal(selectorLocal)
    if (shift != 0) {
        body.buildConstI32(shift)
        body.buildInstr(WasmOp.I32_SUB)
    }
    body.buildInstr(
        WasmOp.BR_TABLE,
        WasmImmediate.LabelIdxVector(brTable),
        WasmImmediate.LabelIdx(branches.size)
    )
    body.buildEnd()

    for (expression in branches) {
        if (resultType != null) {
            body.buildDrop()
        }
        generateWithExpectedType(expression.expression, expectedType)

        body.buildBr(baseBlockIndex + 1)
        body.buildEnd()
    }

    if (elseExpression != null) {
        if (resultType != null) {
            body.buildDrop()
        }
        generateWithExpectedType(elseExpression, expectedType)
    }

    body.buildEnd()
    check(baseBlockIndex == body.numberOfNestedBlocks)
}