// PROBLEM: 'if' has empty body
// FIX: none

fun test(i: Int) {
    if (i == 1) {
    } else <caret>if (i == 2) {
    }
}