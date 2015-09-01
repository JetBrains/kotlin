// "Replace with 'newFun()'" "true"

@Deprecated("", ReplaceWith("newFun()"))
fun oldFun(p: Int) {
    newFun()
}

fun newFun(){}

fun foo() {
    <caret>oldFun(bar())
}

fun bar(): Int = 0
