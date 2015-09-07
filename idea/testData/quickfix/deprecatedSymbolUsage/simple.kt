// "Replace with 'newFun()'" "true"

@Deprecated("", ReplaceWith("newFun()"))
fun oldFun() {
    newFun()
}

fun newFun(){}

fun foo() {
    <caret>oldFun()
}
