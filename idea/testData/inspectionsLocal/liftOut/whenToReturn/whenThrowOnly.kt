// PROBLEM: none

fun foo(): Int {
    <caret>when {
        else -> throw Exception()
    }
}