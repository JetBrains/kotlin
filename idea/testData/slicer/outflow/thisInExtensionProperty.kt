// FLOW: OUT

val String.extensionProp: Any
    get() = this

fun foo() {
    val x = <caret>"".extensionProp
}