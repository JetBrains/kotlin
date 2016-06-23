// ERROR: Expression 'f' of type 'String' cannot be invoked as a function. The function 'invoke()' is not found
// IS_APPLICABLE: false
class Paren(val f: String) {
}

fun nonSimple() {
    Paren("").f(<caret>6)
}