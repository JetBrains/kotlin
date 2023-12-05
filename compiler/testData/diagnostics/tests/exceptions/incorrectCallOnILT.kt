// FIR_IDENTICAL

import D.*

class A {
    object B {
        fun b() {}
    }
}
class D {
    object A {
        fun a() {}
    }
}

object A1 {
    fun a() {}
}

fun main() {
    val x = A // should resolved to D.A
    x.a()
    val y = A.B // should be resolved to A.B
    y.b()
}