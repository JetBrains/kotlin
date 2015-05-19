// "Replace with 'newFun(p, this)'" "true"

interface I

@deprecated("", ReplaceWith("newFun(p, this)"))
fun I.plus(p: Int) {
    newFun(p, this)
}

fun newFun(p: Int, i: I) { }

fun foo(i: I) {
    i <caret>+ 1
}
