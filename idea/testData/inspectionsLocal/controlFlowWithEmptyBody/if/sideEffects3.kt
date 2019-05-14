// PROBLEM: 'if' has empty body
// FIX: none

fun test(i: Int) {
    <caret>if (i == 1) {
    } else if (foo()) {
    }
}

fun foo(): Boolean {
    // do something
    return true
}