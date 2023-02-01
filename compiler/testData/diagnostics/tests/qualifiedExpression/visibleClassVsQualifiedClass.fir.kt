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

fun test(a_: a.b) {
    a_.a_b()

    val a_2 = a.<!UNRESOLVED_REFERENCE!>b<!>()
    a_2.a_b()
    a_2.some_ab()
    a_2.a_()
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
    _ab2.other2_ab()
}

// FILE: c3.kt
package some

fun test(_ab: a.b) {
    _ab.some_ab()

    val _ab2 = a.b()
    _ab2.some_ab()
}
