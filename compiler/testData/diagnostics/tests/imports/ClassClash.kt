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

// MODULE: m3(m1, m2)
// FILE: c.kt
import a.B

fun test(b: B) {
    b.m1()
    b.<!UNRESOLVED_REFERENCE!>m2<!>()

    val b_: B = B()
    b_.m1()

    val b_1: a.B = B()
    b_1.m1()

    val b_2: B = a.B()
    b_2.m1()

    val b_3: B = B()
    b_3.m1()

    val b_4: B = a.B()
    b_4.m1()
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
