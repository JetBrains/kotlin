package p1

fun test(s: String, i: Int) {
    p2.foo(<caret>)

fun foo(i: Int) = i

// EXIST: s