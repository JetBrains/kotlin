// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// MODULE: m1
// FILE: a.kt
package a.b

class c {
    fun ab_c() {}
}

// MODULE: m2
// FILE: b.kt
package a

class b<T> {
    class c {
        fun a_bc() {}
    }
}

// MODULE: m3(m1, m2)
// FILE: c.kt
import a.b.c

fun test(ab_c: c) {
    ab_c.ab_c()

    val ab_c2: a.b.c = a.b.c()
    ab_c2.ab_c()

    val ab_c3 = a.b.c()
    ab_c3.ab_c()
}

// NB according to the maximum package prefix rule, 'a.b.c' denotes 'c' in package 'a.b' (thus ab_c, not a_bc)
fun test2(ab_c: a.b.c) {
    ab_c.<!UNRESOLVED_REFERENCE!>a_bc<!>()
    ab_c.ab_c()
}
