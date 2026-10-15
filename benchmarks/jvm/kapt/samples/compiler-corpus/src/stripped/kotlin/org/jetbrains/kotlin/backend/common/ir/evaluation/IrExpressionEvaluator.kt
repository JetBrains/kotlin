/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir.evaluation

import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.evaluation.IrConstFieldInliner
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.resolve.constants.evaluate.CompileTimeType
import org.jetbrains.kotlin.resolve.constants.evaluate.canEvalOp
import org.jetbrains.kotlin.resolve.constants.evaluate.evalBinaryOp
import org.jetbrains.kotlin.resolve.constants.evaluate.evalUnaryOp
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

/**
 * Evaluates the given IR expression using constant folding.
 * This function returns either
 *  - [IrConst], if exression can be completly folded;
 *  - [IrComposite], if expression is folded, but can't be completly eliminated because it has side effects;
 *  - null, if the expression can't be folded.
 */
fun evaluate(
    expression: IrExpression,
    irFile: IrFile,
    irBuiltIns: IrBuiltIns,
    inlineConstTracker: InlineConstTracker?,
    isFloatingPointOptimizationEnabled: Boolean,
): IrExpression? { TODO() }

private class IrExpressionEvaluator(
    private val irBuiltIns: IrBuiltIns,
    private val isFloatingPointOptimizationEnabled: Boolean,
) : IrVisitor<IrExpression?, Nothing?>() {
    private fun IrExpression.evaluateAsConst(): IrConst? = TODO()

    override fun visitElement(element: IrElement, data: Nothing?): IrExpression? = TODO()

    override fun visitConst(expression: IrConst, data: Nothing?): IrConst = TODO()

    override fun visitCall(expression: IrCall, data: Nothing?): IrExpression? { TODO() }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): IrConst? { TODO() }

    private fun evaluateBuiltinCall(expression: IrCall): IrExpression? { TODO() }

    private fun inlineCallableName(expression: IrCall): IrExpression? { TODO() }

    private fun inlineEnumName(expression: IrCall): IrConst? { TODO() }

    companion object {
        private val IrDeclarationWithName.callableId: CallableId?
            get() {
                return when (val parent = this.parent) {
                    is IrClass -> if (parent.isFacadeClass) {
                        parent.packageFqName?.let { CallableId(it, name) }
                    } else {
                        parent.classId?.let { CallableId(it, name) }
                    }
                    is IrPackageFragment -> CallableId(parent.packageFqName, name)
                    else -> null
                }
            }

        private fun IrType.isFloatOrDouble(): Boolean = TODO()

        private fun IrType.toCompileTimeType(): CompileTimeType? { TODO() }

        private fun IrCall.isCompileTimeBuiltinCall(): Boolean { TODO() }

        private fun IrCall.isExcludedFromEvaluation(): Boolean { TODO() }

        private fun IrDeclaration.fromStdlib(): Boolean { TODO() }

        private fun IrConst.getCastedValue(): Any? { TODO() }

        private fun IrCall.isInterpretableKCallableNameCall(irBuiltIns: IrBuiltIns): Boolean { TODO() }

        private fun IrCall.isEnumName(): Boolean { TODO() }
    }
}
