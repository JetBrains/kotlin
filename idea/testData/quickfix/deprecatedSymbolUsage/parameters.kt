// "Replace with 'newFun(b, a, null)'" "true"

@deprecated("", ReplaceWith("newFun(b, a, null)"))
fun oldFun(a: Int, b: String) {
}

fun newFun(b: String, a: Int, x: Any?){}

fun foo() {
    <caret>oldFun(1, "x")
}
