// PROBLEM: 'if' has empty body
// FIX: Extract side effects

fun test(i: Int) {
    <caret>if (bar() == baz()) {
    } else {
    }
}

fun bar(): Int {
    // do something
    return 1
}

fun baz(): Int {
    // do something
    return 2
}