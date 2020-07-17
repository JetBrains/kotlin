// FLOW: IN

fun Any.extensionFun(): Any {
    return this
}

fun foo() {
    val <caret>x = "".extensionFun()
}