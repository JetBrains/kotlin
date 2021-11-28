// MODULE: lib
// FILE: A.kt

package aaa

class A {
    class B {
        class O {
          val s = "OK"
        }
    }
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    val str = aaa.A.B.O().s
    return str
}
