// "Replace with 'newFun()'" "true"

@deprecated("", ReplaceWith("newFun()"))
fun oldFun() {
    newFun()
}

fun newFun(){}

fun foo() {
    <caret>oldFun()
}
