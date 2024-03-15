// MODULE: lib
// FILE: lib.kt

package a

class A(val x: String)

// MODULE: main(lib)
// FILE: main.kt

import a.*

fun box(): String {
    val p1 = A::x
    val a = A("K")
    val p2 = a::x
    return p1.get(A("O")) + p2.get()
}