// "Replace with 'newFun(p)'" "true"

@deprecated("", ReplaceWith("newFun(p)"))
fun String.oldFun(p: Int) {
    newFun(p)
}

fun String.newFun(p: Int) {
}

fun foo() {
    "" <caret>oldFun 1
}
