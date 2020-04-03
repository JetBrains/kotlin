// FILE: 1.kt
package test

class Z

class Q {
    inline fun f(z: Z) = "OK"
}

// FILE: 2.kt
import test.*

fun box(): String {
    return Z().run(Q()::f)
}