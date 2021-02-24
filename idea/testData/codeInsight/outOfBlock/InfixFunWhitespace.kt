// OUT_OF_CODE_BLOCK: FALSE
// TYPE: ' '
fun main() {
    "x"<caret>foo 4.0
}

infix fun String.foo(s: Double) = Unit