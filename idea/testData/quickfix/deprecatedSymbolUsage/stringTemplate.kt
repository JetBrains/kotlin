// "Replace with '"p = $p"'" "true"

@deprecated("", ReplaceWith("\"p = \$p\""))
fun oldFun(p: Int) = "p = $p"

fun foo(p: Int) {
    val s = <caret>oldFun(p + 1)
}
