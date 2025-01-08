// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// MODULE: top_m1
// FILE: top_a.kt
package a

class b {
    fun a_b() {}
}

// MODULE: top_m2
// FILE: top_b.kt
class a<T> {
    class b {
        fun _ab() {}
    }
    fun _a() {}
}

// MODULE: top_m3(top_m1, top_m2)
// FILE: top_c.kt
import a.b

fun test(a_b: b) {
    a_b.a_b()

    val _ab: a.b = a.b()
    _ab._ab()

    val _ab2 = a.b()
    _ab2._ab()
}

fun test2(_ab: a.b) {
    _ab._ab()
    _ab.<!UNRESOLVED_REFERENCE!>a_b<!>()
}
