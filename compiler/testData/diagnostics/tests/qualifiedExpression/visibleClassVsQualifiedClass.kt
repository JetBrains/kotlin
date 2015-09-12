// MODULE: m1
// FILE: a.kt
package a

class b {
    fun a_b() {}
}

// MODULE: m2
// FILE: b.kt
package some

class a {
    class b {
        fun some_ab() {}
    }
}

// MODULE: m3(m1, m2)
// FILE: c1.kt
package other

class a {}

fun test(a_: a.<!UNRESOLVED_REFERENCE!>b<!>) {
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a_<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a_b<!>()

    val a_2 = a.b()
    a_2.a_b() // todo: must be unresolved
    a_2.<!UNRESOLVED_REFERENCE!>some_ab<!>()
    a_2.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a_()<!>
}

// FILE: c2.kt
package other2

class a {
    class b {
        fun other2_ab() {}
    }
}

fun test(_ab: a.b) {
    _ab.other2_ab()

    val _ab2 = a.b()
    _ab2.<!UNRESOLVED_REFERENCE!>other2_ab<!>() // todo
}

// FILE: c3.kt
package some

fun test(_ab: a.b) {
    _ab.some_ab()

    val _ab2 = a.b()
    _ab2.<!UNRESOLVED_REFERENCE!>some_ab<!>() // todo
}