// FILE: a.kt
package a

class A(val b: String) {
    companion object {
        val c: String = ""
    }

    fun mtd() = c.length
}

// FILE: b.kt
package b

// FILE: c.kt
package c

// FILE: test.kt
package test

import a.A

fun <T, R> T.with(f: T.() -> R) = <!INAPPLICABLE_CANDIDATE!>f<!>()

fun A.extFun1() = b.length

// fun A.extFun2() = c.length // TODO fix KT-9953

val x1 = A("").with { b.<!UNRESOLVED_REFERENCE!>length<!> }

// val x2 = A("").with { c.length } // TODO fix KT-9953

val x3 = A.with { c.<!UNRESOLVED_REFERENCE!>length<!> }