// PROBLEM: none

fun test(i: Int) {
    <caret>do {
        foo()
    } while (i == 1)
}

fun foo() {}