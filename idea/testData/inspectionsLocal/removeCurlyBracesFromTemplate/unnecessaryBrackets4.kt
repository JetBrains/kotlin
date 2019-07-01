
fun foo() {
    val x = 4
    val y = "$<caret>{x}() this is okay, x will not be thought of as a function call"
}