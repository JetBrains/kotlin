// "Replace with 'newFun(p, p)'" "true"

@Deprecated("", ReplaceWith("newFun(p, p)"))
fun oldFun(p: Int?): Int {
    return newFun(p, p)
}

fun newFun(p1: Int?, p2: Int?): Int = 0

fun foo(): Int {
    return <caret>oldFun(bar())
}

fun bar(): Int? = null
