// !WITH_NEW_INFERENCE
// MODULE: m1
// FILE: a.kt
package a

class B {
    fun m1() {}
}

// MODULE: m2
// FILE: b.kt
package a

class B {
    fun m2() {}
}

// MODULE: m3(m2, m1)
// FILE: b.kt
import a.*


fun test(b: B) {
    b.m2()
    b.<!UNRESOLVED_REFERENCE!>m1<!>()

    val b_: B = B()
    b_.m2()

    val b_1: a.B = B()
    b_1.m2()

    val b_2: B = a.B()
    b_2.m2()

    val b_3 = B()
    b_3.m2()

    val b_4 = a.B()
    b_4.m2()
}


//----------------------------TOP LEVEL---------------------

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

    val b_4 = <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>B<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b_4<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>m2<!>()
}