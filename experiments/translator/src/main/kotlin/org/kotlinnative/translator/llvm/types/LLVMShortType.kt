package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue

class LLVMShortType() : LLVMType() {

    override val align = 2
    override var size: Int = 2
    override val defaultValue = "0"
    override val typename = "i16"
    override val mangle = "Short"
    override val isPrimitive = true

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp slt i16 $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sgt i16 $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sle i16 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sge i16 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp eq i16" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp ne i16" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorMod(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMShortType(), "srem i16 $firstOp, $secondOp")

    override fun operatorDiv(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMShortType(), "sdiv i16 $firstOp, $secondOp")

    override fun equals(other: Any?) =
            other is LLVMShortType

    override fun hashCode() =
            mangle.hashCode()

}