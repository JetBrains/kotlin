// PROBLEM: none

val x: Number? = null

fun foo(): Int {
    val <caret>y = x
    if (y is Int) return y
    return 0
}