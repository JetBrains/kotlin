// "Create function 'operate'" "false"
// ACTION: Add 'p =' to argument
// ACTION: Create extension function 'Operated.unaryPlus'
// ACTION: Create member function 'Operated.unaryPlus'
// ACTION: Replace overloaded operator with function call
// DISABLE-ERRORS
class Operated

fun operate(p: Any?) {}

fun combine() {
    operate(<caret>+Operated())
}