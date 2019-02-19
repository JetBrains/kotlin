/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.irasdescriptors.containsNull
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

/** Look for when-constructs where subject is enum entry.
 * Replace branches that are comparisons with compile-time known enum entries
 * with comparisons of ordinals.
 */
internal class EnumWhenLowering(private val context: Context) : IrElementTransformerVoid(), FileLoweringPass {

    private val subjectWithOrdinalStack = mutableListOf<Pair<IrVariable, Lazy<IrVariable>>>()

    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    // Checks that irBlock satisfies all constrains of this lowering.
    // 1. Block's origin is WHEN
    // 2. Subject of `when` is variable of enum type
    // NB: See BranchingExpressionGenerator in Kotlin sources to get insight about
    // `when` block translation to IR.
    private fun shouldLower(irBlock: IrBlock): Boolean {
        if (irBlock.origin != IrStatementOrigin.WHEN) {
            return false
        }
        // when-block with subject should have two children: temporary variable and when itself.
        if (irBlock.statements.size != 2) {
            return false
        }
        val subject = irBlock.statements[0] as IrVariable
        // Subject should not be nullable because we will access the `ordinal` property.
        if (subject.type.containsNull()) {
            return false
        }
        // Check that subject is enum entry.
        val enumClass = subject.type.getClass()
                ?: return false
        return enumClass.kind == ClassKind.ENUM_CLASS
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (!shouldLower(expression)) {
            return super.visitBlock(expression)
        }
        // Will be initialized only when we found a branch that compares
        // subject with compile-time known enum entry.
        val subject = expression.statements[0] as IrVariable
        val subjectOrdinalProvider = lazy {
            createEnumOrdinalVariable(subject)
        }
        subjectWithOrdinalStack.push(Pair(subject, subjectOrdinalProvider))
        // Process nested `when` and comparisons.
        expression.transformChildrenVoid(this)
        // If variable was initialized then it was actually used and we need to insert it
        // into the block's IR.
        if (subjectOrdinalProvider.isInitialized()) {
            expression.statements.add(1, subjectOrdinalProvider.value)
        }
        subjectWithOrdinalStack.pop()
        return expression
    }

    // Create temporary variable for subject's ordinal.
    private fun createEnumOrdinalVariable(enumVariable: IrVariable) = WrappedVariableDescriptor().let {
        IrVariableImpl(
                enumVariable.startOffset, enumVariable.endOffset,
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                IrVariableSymbolImpl(it),
                Name.identifier(enumVariable.name.asString() + "_ordinal"),
                context.irBuiltIns.intType,
                isVar = false,
                isConst = false,
                isLateinit = false
        ).apply {
            it.bind(this)
            parent = enumVariable.parent

            val ordinalPropertyGetter = context.ir.symbols.enum.getPropertyGetter("ordinal")!!
            initializer = IrCallImpl(
                    enumVariable.startOffset, enumVariable.endOffset,
                    ordinalPropertyGetter.owner.returnType,
                    ordinalPropertyGetter
            ).apply {
                dispatchReceiver = IrGetValueImpl(
                        enumVariable.startOffset, enumVariable.endOffset,
                        enumVariable.type, enumVariable.symbol
                )
            }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        // Try to do actual lowering.
        return tryLower(expression)
    }

    private val areEqualByValue = context.irBuiltIns.eqeqSymbol

    // We are looking for branch that is a comparison of the subject and another enum entry.
    private fun tryLower(call: IrCall): IrExpression {
        if (call.origin != IrStatementOrigin.EQEQ) {
            return call
        }
        val callArgs = call.getArguments()
        if (callArgs.size != 2) {
            return call
        }
        val lhs = callArgs[0].second
        val rhs = callArgs[1].second

        // If there is nothing on stack then nothing we can do.
        val (topmostSubject, topmostOrdinalProvider) = subjectWithOrdinalStack.peek()
                ?: return call
        if (lhs is IrValueAccessExpression && lhs.symbol.owner == topmostSubject && rhs is IrGetEnumValue &&
                // Both entries should belong to the same class:
                topmostSubject.type.classifierOrNull?.owner == rhs.symbol.owner.parent) {
            val entryOrdinal = context.specialDeclarationsFactory.getEnumEntryOrdinal(rhs.symbol.owner)
            val subjectOrdinal = topmostOrdinalProvider.value
            return IrCallImpl(call.startOffset, call.endOffset, areEqualByValue.owner.returnType, areEqualByValue).apply {
                putValueArgument(0,
                        IrGetValueImpl(lhs.startOffset, lhs.endOffset, subjectOrdinal.type, subjectOrdinal.symbol))
                putValueArgument(1,
                        IrConstImpl.int(rhs.startOffset, rhs.endOffset, context.irBuiltIns.intType, entryOrdinal))
            }
        }
        return call
    }
}