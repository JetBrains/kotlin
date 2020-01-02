// FILE: A.kt
package lib

interface A {
    fun f(x: String = "OK"): String
}

class B : A {
    override fun f(x: String) = x
}

class C(val x: A) : A by x

// FILE: B.kt
import lib.*

fun box() = C(B()).f()
