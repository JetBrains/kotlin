// PROBLEM: 'when' has empty body
// FIX: Extract side effects

fun test(i: Int) {
    <caret>when (foo()) {
    }
}

fun foo(): Boolean {
    // do something
    return true
}
