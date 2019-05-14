// PROBLEM: 'when' has empty body
// FIX: Extract side effects

fun test(i: Int) {
    <caret>when (val i = foo()) {
    }
}

fun foo(): Boolean {
    // do something
    return true
}