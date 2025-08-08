// DUMP_IR

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0 2.1.0
// ^^^ KT-75983 fixed in 2.2.0-Beta2

sealed interface OperandType<J> {
    fun operand1(instruction: OneOperand<*, *>): J = instruction.operand1 as J
}

sealed interface Instruction

sealed interface OneOperand<J, T : OperandType<J>> : Instruction {
    val operand1: J
    val type: T

    fun operand() = this.operand1
}

object SInt32 : OperandType<Int>

data class LoadConstant<J, T : OperandType<J>>(override val operand1: J, override val type: T) : OneOperand<J, T>

fun byteSize(instruction: LoadConstant<*, *>) {
    if (instruction.type !is SInt32) return
    when (instruction.type.operand1(instruction)) {
        in -1..5 -> {}
    }
    when (val x = instruction.type.operand1(instruction)) {
        in -1..5 -> {}
    }
}

fun box(): String {
    byteSize(LoadConstant(0, SInt32))
    return "OK"
}