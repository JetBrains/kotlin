// FILE: A.kt

package aaa

class A(val a: Int = 1)

// FILE: B.kt

fun box(): String {
    aaa.A()
    return "OK"
}
