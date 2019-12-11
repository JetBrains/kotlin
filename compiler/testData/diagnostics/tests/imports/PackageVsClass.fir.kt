// MODULE: m1
// FILE: a.kt
package a.b

fun ab_fun() {}

class c {
    fun ab_c() {}

    class d {
        fun ab_cd() {}
    }
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
import a.a_fun
import a.b

import a.b.ab_fun
import a.b.c

fun test(a_b: b) {
    a_b.a_b()

    val a_bc: b.c = b.c()
    a_bc.a_bc()

    val a_bc2 = b.c()
    a_bc2.a_bc()

    a_fun()
}

fun test2(ab_c: c) {
    ab_c.ab_c()

    val ab_cd: c.d = c.d()
    ab_cd.ab_cd()

    val ab_cd2 = c.d()
    ab_cd2.ab_cd()

    ab_fun()
}

// FILE: d.kt
package a

import a.b.ab_fun
import a.b.c

fun test(a_b: b) {
    a_b.a_b()

    val a_bc: b.c = b.c()
    a_bc.a_bc()

    val a_bc2 = b.c()
    a_bc2.a_bc()

    a_fun()
}

fun test2(ab_c: c) {
    ab_c.ab_c()

    val ab_cd: c.d = c.d()
    ab_cd.ab_cd()

    val ab_cd2 = c.d()
    ab_cd2.ab_cd()

    ab_fun()
}

//---- Changed dependence order
// MODULE: m4(m2, m1)
// FILE: c.kt
import a.a_fun
import a.b

import a.b.ab_fun
import a.b.c

fun test(a_b: b) {
    a_b.a_b()

    val a_bc: b.c = b.c()
    a_bc.a_bc()

    val a_bc2 = b.c()
    a_bc2.a_bc()

    a_fun()
}

fun test2(ab_c: c) {
    ab_c.ab_c()

    val ab_cd: c.d = c.d()
    ab_cd.ab_cd()

    val ab_cd2 = c.d()
    ab_cd2.ab_cd()

    ab_fun()
}

// FILE: d.kt
package a

import a.b.ab_fun
import a.b.c

fun test(a_b: b) {
    a_b.a_b()

    val a_bc: b.c = b.c()
    a_bc.a_bc()

    val a_bc2 = b.c()
    a_bc2.a_bc()

    a_fun()
}

fun test2(ab_c: c) {
    ab_c.ab_c()

    val ab_cd: c.d = c.d()
    ab_cd.ab_cd()

    val ab_cd2 = c.d()
    ab_cd2.ab_cd()

    ab_fun()
}