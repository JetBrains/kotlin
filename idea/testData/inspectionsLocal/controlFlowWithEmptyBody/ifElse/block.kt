// PROBLEM: 'else' has empty body
// FIX: none

fun test(i: Int) {
    if (i == 1) {
    } <caret>else {
    }
}