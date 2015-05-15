// "Replace with 'newFun(p1 + p2)'" "true"

@deprecated("", ReplaceWith("newFun(p1 + p2)"))
fun oldFun(p1: Int, p2: Int) {}

fun newFun(n: Int) {}

fun foo() {
    <caret>oldFun(1, 2)
}
