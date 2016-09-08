package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue


class LLVMDoubleType() : LLVMType() {

    override val align = 8
    override var size: Int = 8
    override val mangle = "Double"
    override val typename = "double"
    override val defaultValue = "0.0"
    override val isPrimitive = true

    override fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMDoubleType(), "fsub double $firstOp, $secondOp")

    override fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMDoubleType(), "fmul double $firstOp, $secondOp")

    override fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMDoubleType(), "fadd double $firstOp, $secondOp")

    override fun operatorInc(firstOp: LLVMSingleValue) =
            LLVMExpression(LLVMDoubleType(), "fadd double $firstOp, 1.0")

    override fun operatorDiv(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMDoubleType(), "fdiv double $firstOp, $secondOp")

    override fun operatorDec(firstOp: LLVMSingleValue) =
            LLVMExpression(LLVMDoubleType(), "fsub double $firstOp, 1.0")

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp olt double $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp ogt double $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp ole double i32 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp oge double i32 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp oeq double $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp one double $firstOp, $secondOp")

    override fun operatorMod(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMDoubleType(), "frem double $firstOp, $secondOp")

    override fun equals(other: Any?) =
            other is LLVMDoubleType

    override fun hashCode() =
            mangle.hashCode()

}