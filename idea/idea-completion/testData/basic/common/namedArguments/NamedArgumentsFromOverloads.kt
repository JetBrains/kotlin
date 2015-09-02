package test

val nLocal = 12

class foo(nClassParam: String, val nClassField: String)
fun foo(nFirst: String) = 12
fun foo(nSecond: String? = null, nThird: Int = 1) { }

fun other() {
    foo(n<caret>)
}

// EXIST: nLocal
// EXIST: { lookupString:"nFirst", itemText:"nFirst =", tailText: " String" }
// EXIST: { itemText: "nClassParam =", tailText: " String" }
// EXIST: { itemText: "nClassField =", tailText: " String" }
// EXIST: { itemText: "nSecond =", tailText: " String?" }
// EXIST: { itemText: "nThird =", tailText: " Int" }
