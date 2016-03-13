// FILE: A.kt

package aaa

class A {
    class O {
        val s = "OK"
    }
}

// FILE: B.kt

fun box(): String {
    return aaa.A.O().s
}
