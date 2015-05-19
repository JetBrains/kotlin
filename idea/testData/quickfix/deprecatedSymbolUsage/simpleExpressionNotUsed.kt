// "Replace with 'newFun()'" "true"

@deprecated("", ReplaceWith("newFun()"))
fun oldFun(p: Int) {
    newFun()
}

fun newFun(){}

fun foo() {
    <caret>oldFun(O.x + 1)
}

object O {
    var x = 0
}
