// MODULE: m1
// FILE: a.kt
package a.b

class c {
    fun ab_c() {}
}

// MODULE: m2
// FILE: b.kt
package a

fun a_fun() {}

class b {
    fun a_b() {}

    class c {
        fun a_bc() {}
    }
}

// MODULE: m3(m1, m2)
// FILE: c.kt

fun test(a_b: a.b) {
    a_b.a_b()

    val ab_c: a.b.c = a.b.c()
    ab_c.ab_c()
    ab_c.<!UNRESOLVED_REFERENCE!>a_bc<!>()

    val ab_c2 = a.b.c()
    ab_c2.ab_c()
    ab_c2.<!UNRESOLVED_REFERENCE!>a_bc<!>()
}

fun test2(ab_c: a.b.c) {
    ab_c.ab_c()
}

//---- Changed dependence order
// MODULE: _m1
// FILE: _a.kt
package a.b

fun ab_fun() {}

class c {
    fun ab_c() {}

    class d {
        fun ab_cd() {}
    }
}

// MODULE: _m2
// FILE: _b.kt
package a

fun a_fun() {}

class b {
    fun a_b() {}

    class c {
        fun a_bc() {}
    }
}

// MODULE: _m3(_m1, _m2)
// FILE: _c.kt

fun test(a_b: a.b) {
    a_b.a_b()

    val ab_c: a.b.c = a.b.c()
    ab_c.ab_c()
    ab_c.<!UNRESOLVED_REFERENCE!>a_bc<!>()

    val ab_c2 = a.b.c()
    ab_c2.ab_c()
    ab_c2.<!UNRESOLVED_REFERENCE!>a_bc<!>()
}

fun test2(ab_c: a.b.c) {
    ab_c.ab_c()
}