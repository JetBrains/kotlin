// PROBLEM: 'for' has empty body
// FIX: none

fun test() {
    <caret>for (i in 1..10) {
        // comment
    }
}