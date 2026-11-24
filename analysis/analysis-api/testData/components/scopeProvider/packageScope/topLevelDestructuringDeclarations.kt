// KT-90376
// FILE: declarations.kt
package test

data class X(val a: Int, val b: String)

val (a, b) = X(1, "")

var [c, d] = X(2, "")

val e = 3

class C {
    val (memberA, memberB) = X(3, "")
}

// FILE: main.kt
// package: test
