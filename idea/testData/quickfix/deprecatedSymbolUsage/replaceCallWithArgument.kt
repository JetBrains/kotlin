// "Replace with 'p'" "true"
deprecated("", ReplaceWith("p"))
fun oldFun(p: Int): Int = p

fun foo() {
    <caret>oldFun(0)
}
