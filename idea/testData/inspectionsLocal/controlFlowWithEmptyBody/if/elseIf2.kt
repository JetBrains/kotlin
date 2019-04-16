// PROBLEM: 'if' has empty body

fun test(i: Int) {
    if (i == 1) {
    } else <caret>if (i == 2) {
    } else {
    }
}

fun foo() {}