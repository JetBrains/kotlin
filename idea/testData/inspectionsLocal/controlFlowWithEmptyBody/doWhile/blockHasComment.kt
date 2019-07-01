// PROBLEM: 'do while' has empty body
// FIX: none

fun test(i: Int) {
    <caret>do {
        // comment
    } while (i == 1)
}