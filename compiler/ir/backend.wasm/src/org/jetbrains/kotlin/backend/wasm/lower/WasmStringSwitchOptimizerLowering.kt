/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.IrWhenUtils
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.isElseBranch
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

private object OPTIMISED_WHEN_SUBJECT : IrDeclarationOriginImpl("OPTIMISED_WHEN_SUBJECT")

class WasmStringSwitchOptimizerLowering(
    private val context: WasmBackendContext
) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    private val symbols = context.wasmSymbols

    private val stringHashCode by lazy {
        symbols.irBuiltIns.stringClass.getSimpleFunction("hashCode")!!
    }

    private val intType: IrType = symbols.irBuiltIns.intType
    private val booleanType: IrType = symbols.irBuiltIns.booleanType

    private fun IrBlockBuilder.createEqEqForIntVariable(tempIntVariable: IrVariable, value: Int) =
        irCall(context.irBuiltIns.eqeqSymbol, booleanType).also {
            it.putValueArgument(0, irGet(tempIntVariable))
            it.putValueArgument(1, value.toIrConst(intType))
        }

    private fun asEqCall(expression: IrExpression): IrCall? =
        (expression as? IrCall)?.takeIf { it.symbol == context.irBuiltIns.eqeqSymbol }

    private class MatchedCase(val condition: IrCall, val branchIndex: Int)
    private class BucketSelector(val hashCode: Int, val selector: IrExpression)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    private fun tryMatchCaseToNullableStringConstant(condition: IrExpression): IrConst<*>? {
        val eqCall = asEqCall(condition) ?: return null
        if (eqCall.valueArgumentsCount < 2) return null
        val constantReceiver =
            eqCall.getValueArgument(0) as? IrConst<*>
                ?: eqCall.getValueArgument(1) as? IrConst<*>
                ?: return null
        return when (constantReceiver.kind) {
            IrConstKind.String, IrConstKind.Null -> constantReceiver
            else -> null
        }
    }

    private fun IrBlockBuilder.addHashCodeVariable(firstEqCall: IrCall): IrVariable {
        val subject: IrExpression
        val subjectArgumentIndex: Int
        val firstArgument = firstEqCall.getValueArgument(0)!!
        if (firstArgument is IrConst<*>) {
            subject = firstEqCall.getValueArgument(1)!!
            subjectArgumentIndex = 1
        } else {
            subject = firstArgument
            subjectArgumentIndex = 0
        }

        val subjectType = subject.type

        val whenSubject = buildVariable(
            scope.getLocalDeclarationParent(),
            startOffset,
            endOffset,
            OPTIMISED_WHEN_SUBJECT,
            Name.identifier("tmp_when_subject"),
            subjectType,
        )

        whenSubject.initializer = subject
        +whenSubject
        firstEqCall.putValueArgument(subjectArgumentIndex, irGet(whenSubject))

        val tmpIntWhenSubject = buildVariable(
            scope.getLocalDeclarationParent(),
            startOffset,
            endOffset,
            OPTIMISED_WHEN_SUBJECT,
            Name.identifier("tmp_int_when_subject"),
            intType,
        )

        val getHashCode = irCall(stringHashCode, intType).also {
            it.dispatchReceiver = irGet(whenSubject)
        }

        val hashCode: IrExpression = if (subjectType.isNullable()) {
            val stringIsNull = irCall(context.irBuiltIns.eqeqeqSymbol, booleanType).also {
                it.putValueArgument(0, irGet(whenSubject))
                it.putValueArgument(1, irNull(subjectType))
            }
            irIfThenElse(intType, stringIsNull, 0.toIrConst(intType), getHashCode)
        } else {
            getHashCode
        }

        tmpIntWhenSubject.initializer = hashCode
        +tmpIntWhenSubject

        return tmpIntWhenSubject
    }

    /**
     * Create simple 1-element buckets (for when without else block and commas)
     * when(a) {
     *  "123" -> 123
     *  "456" -> 456
     *  "789" -> 789
     *  }
     *  into the integer when's collections of
     *  48690 -> if(a == "123") -> 123
     *  51669 -> if(a == "456") -> 456
     *  54648 -> if(a == "789") -> 789
     */
    private fun IrBlockBuilder.createSimpleBucketSelectors(
        stringConstantToMatchedCase: Map<String?, MatchedCase>,
        buckets: Map<Int, List<String?>>,
        transformedWhen: IrWhen,
    ): List<BucketSelector> = buckets.entries.map { bucket ->
        val selector = if (bucket.value.size == 1) {
            val bucketCase = bucket.value[0]
            val matchedCase = stringConstantToMatchedCase.getValue(bucketCase)
            irIfThen(
                type = transformedWhen.type,
                condition = matchedCase.condition,
                thenPart = transformedWhen.branches[matchedCase.branchIndex].result,
            )
        } else {
            val bucketBranches = mutableListOf<IrBranch>()
            bucket.value.mapTo(bucketBranches) { bucketCase ->
                val matchedCase = stringConstantToMatchedCase.getValue(bucketCase)
                irBranch(matchedCase.condition, transformedWhen.branches[matchedCase.branchIndex].result)
            }
            irWhen(transformedWhen.type, bucketBranches)
        }
        BucketSelector(bucket.key, selector)
    }

    private fun IrBlockBuilder.createWhenForBucketSelectors(
        tempIntVariable: IrVariable,
        bucketsSelectors: List<BucketSelector>,
        selectorsType: IrType,
        elseBranchExpression: IrExpression?
    ): IrWhen {
        val allBucketsWhenBranches = mutableListOf<IrBranch>()
        bucketsSelectors.mapTo(allBucketsWhenBranches) { bucketSelector ->
            val condition = createEqEqForIntVariable(tempIntVariable, bucketSelector.hashCode)
            irBranch(condition, bucketSelector.selector)
        }
        if (elseBranchExpression != null) {
            allBucketsWhenBranches.add(irElseBranch(elseBranchExpression))
        }
        return irWhen(selectorsType, allBucketsWhenBranches)
    }

    /**
     * Create multi-element buckets for every hashCode
     * 48690 -> when(a) {
     *   "123" -> 0
     *   "ARcZguv123" -> 1
     *   else -> 3
     * }
     * 51669 -> when(a) {
     *   "456" -> 0
     *   else -> 3
     * }
     * 54648 -> when(a) {
     *   "789" -> 1
     *   else -> 3
     * }
     * else -> 3
     */
    private fun IrBlockBuilder.createBucketSelectors(
        stringConstantToMatchedCase: Map<String?, MatchedCase>,
        buckets: Map<Int, List<String?>>,
        elseBranchIndex: Int,
    ): List<BucketSelector> = buckets.entries.map { bucket ->
        val selector = if (bucket.value.size == 1) {
            val bucketCase = bucket.value[0]
            val matchedCase = stringConstantToMatchedCase.getValue(bucketCase)
            irIfThenElse(
                type = intType,
                condition = matchedCase.condition,
                thenPart = matchedCase.branchIndex.toIrConst(intType),
                elsePart = elseBranchIndex.toIrConst(intType)
            )
        } else {
            val bucketBranches = mutableListOf<IrBranch>()
            bucket.value.mapTo(bucketBranches) { bucketCase ->
                val matchedCase = stringConstantToMatchedCase.getValue(bucketCase)
                irBranch(matchedCase.condition, matchedCase.branchIndex.toIrConst(intType))
            }
            bucketBranches.add(irElseBranch(elseBranchIndex.toIrConst(intType)))
            irWhen(intType, bucketBranches)
        }
        BucketSelector(bucket.key, selector)
    }

    private fun IrBlockBuilder.createTransformedWhen(tempIntVariable: IrVariable, transformedWhen: IrWhen): IrWhen {
        val mainResultsBranches = mutableListOf<IrBranch>()
        transformedWhen.branches.mapIndexedTo(mainResultsBranches) { index, branch ->
            if (!isElseBranch(branch)) {
                irBranch(createEqEqForIntVariable(tempIntVariable, index), branch.result)
            } else {
                branch
            }
        }
        return irWhen(transformedWhen.type, mainResultsBranches)
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        val visitedWhen = super.visitWhen(expression) as IrWhen
        if (visitedWhen.branches.size <= 2) return visitedWhen

        var firstEqCall: IrCall? = null
        var isSimpleWhen = true //simple when is when without else block and commas
        val stringConstantToMatchedCase = mutableMapOf<String?, MatchedCase>()
        visitedWhen.branches.forEachIndexed { branchIndex, branch ->
            if (!isElseBranch(branch)) {
                val conditions = IrWhenUtils.matchConditions(context.irBuiltIns.ororSymbol, branch.condition) ?: return visitedWhen
                if (conditions.isEmpty()) return visitedWhen

                isSimpleWhen = isSimpleWhen && conditions.size == 1
                for (condition in conditions) {
                    val matchedStringConstant = tryMatchCaseToNullableStringConstant(condition) ?: return visitedWhen
                    val matchedString = matchedStringConstant.value as? String
                    if (matchedString !in stringConstantToMatchedCase) {
                        stringConstantToMatchedCase[matchedString] = MatchedCase(condition, branchIndex)
                        firstEqCall = firstEqCall ?: asEqCall(condition)
                    }
                }
            } else {
                isSimpleWhen = false
            }
        }

        if (firstEqCall == null || stringConstantToMatchedCase.size < 2) return visitedWhen

        val convertedBlock = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).run {
            irBlock(resultType = visitedWhen.type) {
                val tempIntVariable = addHashCodeVariable(firstEqCall!!)

                val buckets = stringConstantToMatchedCase.keys.groupBy { it.hashCode() }

                if (isSimpleWhen) {
                    val bucketsSelectors = createSimpleBucketSelectors(
                        stringConstantToMatchedCase = stringConstantToMatchedCase,
                        buckets = buckets,
                        transformedWhen = expression
                    )
                    +createWhenForBucketSelectors(
                        tempIntVariable = tempIntVariable,
                        bucketsSelectors = bucketsSelectors,
                        selectorsType = expression.type,
                        elseBranchExpression = null
                    )
                } else {
                    val elseBranchIndex = expression.branches.size
                    val bucketsSelectors = createBucketSelectors(
                        stringConstantToMatchedCase = stringConstantToMatchedCase,
                        buckets = buckets,
                        elseBranchIndex = elseBranchIndex
                    )
                    val caseSelectorWhen = createWhenForBucketSelectors(
                        tempIntVariable = tempIntVariable,
                        bucketsSelectors = bucketsSelectors,
                        selectorsType = intType,
                        elseBranchExpression = elseBranchIndex.toIrConst(intType)
                    )
                    +irSet(tempIntVariable, caseSelectorWhen)
                    +createTransformedWhen(tempIntVariable, expression)
                }
            }
        }

        return convertedBlock
    }
}

