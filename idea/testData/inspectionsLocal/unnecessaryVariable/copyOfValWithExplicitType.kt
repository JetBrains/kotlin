// PROBLEM: none

fun test(): Int {
    val x = 1
    // With explicitly given type looks dangerous
    val <caret>y: Int = x
    return x + y
}