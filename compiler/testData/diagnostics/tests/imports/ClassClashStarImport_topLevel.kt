// RUN_PIPELINE_TILL: FRONTEND
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

// MODULE: top_m3(top_m2, top_m1)
// FILE: top_c.kt

fun test(b: B) {
    b.m2()
    b.<!UNRESOLVED_REFERENCE!>m1<!>()

    val b_: B = B()
    b_.m2()

    val b_2 = B()
    b_2.m2()

    val b_3 = B()
    b_3.m2()

    val b_4 = <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>B<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b_4<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>m2<!>()
}
