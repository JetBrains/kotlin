// PROBLEM: 'if' has empty body
// FIX: none

fun test(i: Int) {
    <caret>if (i == 1) {
        // comment
    }
}