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
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
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
): IrExpression? {
    val inlineResult = expression.accept(IrConstFieldInliner(irFile, inlineConstTracker), null)
    val evaluationResult = (inlineResult ?: expression).accept(IrExpressionEvaluator(irBuiltIns, isFloatingPointOptimizationEnabled), null)
    return evaluationResult ?: inlineResult
}

private class IrExpressionEvaluator(
    private val irBuiltIns: IrBuiltIns,
    private val isFloatingPointOptimizationEnabled: Boolean,
) : IrVisitor<IrExpression?, Nothing?>() {
    private fun IrExpression.evaluateAsConst(): IrConst? = this.accept(this@IrExpressionEvaluator, null) as? IrConst

    override fun visitElement(element: IrElement, data: Nothing?): IrExpression? = null

    override fun visitConst(expression: IrConst, data: Nothing?): IrConst = expression

    override fun visitCall(expression: IrCall, data: Nothing?): IrExpression? {
        return when {
            expression.isInterpretableKCallableNameCall(irBuiltIns) -> inlineCallableName(expression)
            expression.isEnumName() -> inlineEnumName(expression)
            expression.isCompileTimeBuiltinCall() -> evaluateBuiltinCall(expression)
            else -> null
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): IrConst? {
        val builder = StringBuilder()
        for (argument in expression.arguments) {
            val const = argument.evaluateAsConst() ?: return null
            if (!isFloatingPointOptimizationEnabled && const.isFloatOrDouble()) return null
            builder.append(const.getCastedValue() ?: return null)
        }
        return IrConstImpl.string(expression.startOffset, expression.endOffset, expression.type, builder.toString())
    }

    private fun evaluateBuiltinCall(expression: IrCall): IrExpression? {
        val owner = expression.symbol.owner
        val name = owner.name.asString()
        val operands = expression.arguments.mapNotNull { argument ->
            if (argument == null) return@mapNotNull null
            val const = argument.evaluateAsConst() ?: return null
            if (!isFloatingPointOptimizationEnabled && const.isFloatOrDouble()) return null
            const
        }

        val computed: Any? = try {
            when (operands.size) {
                1 -> {
                    val type = owner.parameters[0].type.toCompileTimeType() ?: return null
                    val value = operands[0].getCastedValue() ?: return null
                    evalUnaryOp(name, type, value)
                }
                2 -> {
                    val leftType = owner.parameters[0].type.toCompileTimeType() ?: return null
                    val rightType = owner.parameters[1].type.toCompileTimeType() ?: return null
                    val left = operands[0].getCastedValue() ?: return null
                    val right = operands[1].getCastedValue() ?: return null
                    evalBinaryOp(name, leftType, left, rightType, right)
                }
                else -> return null
            }
        } catch (e: Exception) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            // The operation would fail at runtime; leave the expression unfolded.
            return null
        }

        if (computed == null) return null
        return computed.toIrConstOrNull(expression.type, expression.startOffset, expression.endOffset)?.takeUnless {
            !isFloatingPointOptimizationEnabled && it.isFloatOrDouble()
        }
    }

    private fun inlineCallableName(expression: IrCall): IrExpression? {
        val callableReference = expression.dispatchReceiver
        if (callableReference !is IrRichCallableReference<*>) return null

        val boundArgs = callableReference.boundValues.toList() // make a copy
        val owner = callableReference.reflectionTargetSymbol?.owner as? IrDeclarationWithName

        val name = owner?.name?.asString() ?: return null
        val constName = IrConstImpl.string(expression.startOffset, expression.endOffset, irBuiltIns.stringType, name)

        val boundArgsWithSideEffects = boundArgs.filterNot { it is IrGetValue || it is IrConst }
        if (boundArgsWithSideEffects.isEmpty()) return constName

        return IrCompositeImpl(
            expression.startOffset, expression.endOffset,
            expression.type, origin = null, statements = boundArgsWithSideEffects + listOf(constName)
        )
    }

    private fun inlineEnumName(expression: IrCall): IrConst? {
        val enumValue = expression.dispatchReceiver as? IrGetEnumValue ?: return null
        val enumEntry = enumValue.symbol.owner
        return IrConstImpl.string(expression.startOffset, expression.endOffset, irBuiltIns.stringType, enumEntry.name.asString())
    }

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

        private fun IrConst.isFloatOrDouble() = kind == IrConstKind.Double || kind == IrConstKind.Float

        private fun IrType.toCompileTimeType(): CompileTimeType? {
            if (this.isAny() || type.isNullableAny()) return CompileTimeType.ANY
            return when (getPrimitiveType()) {
                PrimitiveType.BOOLEAN -> CompileTimeType.BOOLEAN
                PrimitiveType.CHAR -> CompileTimeType.CHAR
                PrimitiveType.BYTE -> CompileTimeType.BYTE
                PrimitiveType.SHORT -> CompileTimeType.SHORT
                PrimitiveType.INT -> CompileTimeType.INT
                PrimitiveType.LONG -> CompileTimeType.LONG
                PrimitiveType.FLOAT -> CompileTimeType.FLOAT
                PrimitiveType.DOUBLE -> CompileTimeType.DOUBLE
                null -> when (getUnsignedType()) {
                    UnsignedType.UBYTE -> CompileTimeType.UBYTE
                    UnsignedType.USHORT -> CompileTimeType.USHORT
                    UnsignedType.UINT -> CompileTimeType.UINT
                    UnsignedType.ULONG -> CompileTimeType.ULONG
                    null -> when {
                        isString() -> CompileTimeType.STRING
                        else -> null
                    }
                }
            }
        }

        private fun IrCall.isCompileTimeBuiltinCall(): Boolean {
            if (isExcludedFromEvaluation()) return false

            val owner = this.symbol.owner
            val receiverType = owner.parameters.getOrNull(0)?.type?.toCompileTimeType()
            val firstArgType = owner.parameters.getOrNull(1)?.type?.toCompileTimeType()

            val inBuiltinMap = canEvalOp(
                callableId = owner.callableId ?: return false,
                typeA = receiverType,
                typeB = firstArgType
            )
            return inBuiltinMap
        }

        private fun IrCall.isExcludedFromEvaluation(): Boolean {
            val owner = this.symbol.owner
            if (!owner.fromStdlib()) return true

            val callableId = owner.callableId?.toString() ?: return true
            // `lowercase` and `uppercase` have different result on different platforms
            if (callableId == "kotlin/text/lowercase" || callableId == "kotlin/text/uppercase") return true
            return false
        }

        private fun IrDeclaration.fromStdlib(): Boolean {
            return this.getPackageFragment().packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
        }

        private fun IrConst.getCastedValue(): Any? {
            if (value == null) return null
            val constType = this.type.makeNotNull()
            return when (kind) {
                IrConstKind.Null -> null
                IrConstKind.Boolean -> this.value as Boolean
                IrConstKind.Char -> this.value as Char
                IrConstKind.Byte -> if (constType.isUByte()) (this.value as Byte).toUByte() else (this.value as Byte)
                IrConstKind.Short -> if (constType.isUShort()) (this.value as Short).toUShort() else (this.value as Short)
                IrConstKind.Int -> if (constType.isUInt()) (this.value as Int).toUInt() else (this.value as Int)
                IrConstKind.Long -> if (constType.isULong()) (this.value as Long).toULong() else (this.value as Long)
                IrConstKind.Double -> this.value as Double
                IrConstKind.Float -> this.value as Float
                IrConstKind.String -> this.value as String
            }
        }

        private fun IrCall.isInterpretableKCallableNameCall(irBuiltIns: IrBuiltIns): Boolean {
            val receiver = this.dispatchReceiver
            if (receiver !is IrRichCallableReference<*>) {
                return false
            }

            if (receiver.reflectionTargetLinkageError != null) {
                // There was a partial linkage error of reflectionTargetSymbol -> we don't have accurate information about the callable's name.
                return false
            }

            val directMember = this.symbol.owner.propertyIfAccessor

            val irClass = directMember.parent as? IrClass ?: return false
            if (!irClass.isSubclassOf(irBuiltIns.kCallableClass.owner)) return false

            val name = when (directMember) {
                is IrSimpleFunction -> directMember.name
                is IrProperty -> directMember.name
                else -> return false
            }
            return name.asString() == "name"
        }

        private fun IrCall.isEnumName(): Boolean {
            val owner = this.symbol.owner
            if (!owner.hasShape(dispatchReceiver = true, regularParameters = 0)) return false
            val property = owner.correspondingPropertySymbol?.owner ?: return false
            return this.dispatchReceiver is IrGetEnumValue && property.name.asString() == "name"
        }
    }
}
