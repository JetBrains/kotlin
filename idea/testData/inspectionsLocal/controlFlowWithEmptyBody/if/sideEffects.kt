// PROBLEM: 'if' has empty body
// FIX: Extract side effects

fun test(i: Int) {
    <caret>if (foo()) {
    } else if (i == 2) {
    }
}

fun foo(): Boolean {
    // do something
    return true
}