// PROBLEM: none

fun foo(x: Boolean): (Int) -> String {
    <caret>when (x) {
        true -> return { it.toString() }
        else -> return { "42" }
    }
}