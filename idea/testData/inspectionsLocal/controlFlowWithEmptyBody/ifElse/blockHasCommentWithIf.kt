// PROBLEM: none
// FIX: none

fun test(i: Int) {
    if (i == 1) {
        foo()
    } <caret>else {
        // comment
    }
}

fun foo() {}