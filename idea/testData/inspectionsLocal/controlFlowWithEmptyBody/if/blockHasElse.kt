// PROBLEM: 'if' has empty body
// FIX: none

fun test(i: Int) {
    <caret>if (i == 1) {
    } else if (i == 2) {
    } else if (i == 3) {
    } else {
    }
}