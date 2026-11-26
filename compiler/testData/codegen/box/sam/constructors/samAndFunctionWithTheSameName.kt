// MODULE: m1
// FILE: lib.kt
package q

fun interface F {
    fun foo(i: Int): String
}

// MODULE: m2(m1)
// FILE: main.kt
package p

var s: String = ""

fun interface A {
    fun bar()
}

fun A(block: () -> Unit): A = object : A { override fun bar() { block(); s += "O" } }

typealias T = q.F

fun T(block: (Int) -> String): T {
    s += "K"
    return q.F(block)
}

fun box(): String {
    A { }.bar()
    T { it.toString() }

    return s
}
