// FILE: a.kt

package test

open class A {
    val v = "OK"

    open inner class AA {
        protected inline fun inAA(crossinline modifier: (String) -> String): String = modifier(v)
    }
}

// FILE: b.kt

import test.*

class B : A() {
    inner class BB : AA() {
        fun test(): String = inAA { i -> i }
    }
}

fun box(): String = B().BB().test()
