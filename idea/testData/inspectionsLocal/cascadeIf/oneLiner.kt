// PROBLEM: none

fun foo(a: Boolean, b: Boolean): Int {
    return <caret>if (a) 42 else if (b) 13 else 0
}
