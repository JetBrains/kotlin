package test

val nLocal = 12

class foo(nClassParam: String, val nClassField: String)
fun foo(nFirst: String) = 12
fun foo(nSecond: String? = null, nThird: Int = 1) { }

fun other() {
    foo(n<caret>)
}

// EXIST: nFirst
// EXIST: nSecond
// EXIST: nThird
// EXIST: nLocal

// todo - should exist
// ABSENT: nClassParam
// ABSENT: nClassField