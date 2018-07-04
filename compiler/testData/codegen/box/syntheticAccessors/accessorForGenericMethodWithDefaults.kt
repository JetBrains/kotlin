// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// FILE: test.kt
import b.B

fun box() =
        B().getOK()

// FILE: a.kt
package a

open class A<T> {
    protected fun getO(x: T, z: String = "") = "O" + z
    protected fun getK(x: T, z: String = "") = "K" + z
}

// FILE: b.kt
package b

import a.A

class B : A<Long>() {
    inner class Inner {
        fun innerGetO() = getO(0L)
    }

    fun lambdaGetK() = { -> getK(0L) }

    fun getOK() =
            Inner().innerGetO() + lambdaGetK().invoke()
}