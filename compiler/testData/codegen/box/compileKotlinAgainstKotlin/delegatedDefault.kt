// MODULE: lib
// JVM_ABI_K1_K2_DIFF: KT-65534
// FILE: A.kt
package lib

interface A {
    fun f(x: String = "OK"): String
}

class B : A {
    override fun f(x: String) = x
}

class C(val x: A) : A by x

// MODULE: main(lib)
// FILE: B.kt
import lib.*

fun box() = C(B()).f()
