package test

val nLocal = 12

class foo(nClassParam: String, val nClassField: String)
fun foo(nFirst: String) = 12
fun foo(nSecond: String? = null, nThird: Int = 1) { }

fun other() {
    foo(n<caret>)
}

// EXIST: {"lookupString":"nFirst = ","itemText":"nFirst = "}
// EXIST: {"lookupString":"nSecond = ","itemText":"nSecond = "}
// EXIST: {"lookupString":"nThird = ","itemText":"nThird = "}

// todo - should exist
// ABSENT: {"lookupString":"nClassParam = ","itemText":"nClassParam = "}
// ABSENT: {"lookupString":"nClassField = ","itemText":"nClassField = "}