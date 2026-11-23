// MODULE: m1
// FILE: f1.kt
package p1

inline value class A(open val x: String)

fun useA(a: A) = ""

// MODULE: m2(m1)
// FILE: f2.kt
package p2

import p1.A

inline value class B(open val x: Char)

fun useA(a: A) = a.x
fun useB(b: B) = b.x

fun box(): String {
    return p1.useA(A("~~~")) + p2.useA(A("O")) + useB(B('K'))
}
