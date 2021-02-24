// FLOW: OUT

fun String.extensionFun(): Any = this

fun foo() {
    val x = <caret>"".extensionFun()
}