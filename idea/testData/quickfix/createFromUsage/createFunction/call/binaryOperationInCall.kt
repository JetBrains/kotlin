// "Create function 'operate'" "false"
// ACTION: Add 'p =' to argument
// ACTION: Create extension function 'Operated.plus'
// ACTION: Create member function 'Operated.plus'
// ACTION: Flip '+'
// ACTION: Replace overloaded operator with function call
// DISABLE-ERRORS
class OperandA
class OperandB
class OperandC
class Operated {
    operator fun plus(o: OperandA) {}
    operator fun plus(o: OperandB) {}
}

fun operate(p: Any?) {}

fun combine() {
    operate(Operated() + OperandA())
    operate(Operated() <caret>+ OperandC())
}