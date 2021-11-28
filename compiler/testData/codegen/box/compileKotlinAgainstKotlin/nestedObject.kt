// MODULE: lib
// FILE: A.kt

package aaa

class A {
    object O {
        val s = "OK"
    }
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    return aaa.A.O.s
}
