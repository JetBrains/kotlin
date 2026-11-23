// LANGUAGE: +FullValueClasses
// MODULE: m1
// FILE: f1.kt
package p1

value class A(val y: Int, open val x: String)

fun useA(a: A) = ""

// MODULE: m2(m1)
// FILE: f2.kt
package p2

import p1.A

value class B(open val x: Char)

fun useA(a: A) = a.x
fun useB(b: B) = b.x

fun box(): String {
    return p1.useA(A(x = "~~~", y = 42)) + p2.useA(A(33, "O")) + useB(B('K'))
}
