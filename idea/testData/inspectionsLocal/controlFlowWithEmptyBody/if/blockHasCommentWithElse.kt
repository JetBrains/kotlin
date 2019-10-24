// PROBLEM: none

fun test(i: Int) {
    <caret>if (i == 1) {
        // comment
    } else {
        return
    }
    foo()
}

fun foo() {}