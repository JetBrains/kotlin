// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

// MODULE: top_m1
// FILE: top_a.kt
class B {
    fun m1() {}
}

// MODULE: top_m2
// FILE: top_b.kt
class B {
    fun m2() {}
}

// MODULE: top_m3(top_m1, top_m2)
// FILE: top_c.kt
import B

fun test(b: B) {
    b.m1()
    b.<!UNRESOLVED_REFERENCE!>m2<!>()

    val b_: B = B()
    b_.m1()

    val b_2 = B()
    b_2.m1()
}

// FILE: top_d.kt

fun test2(b: B) {
    b.m1()
    b.<!UNRESOLVED_REFERENCE!>m2<!>()

    val b_: B = B()
    b_.m1()

    val b_2 = B()
    b_2.m1()
}
