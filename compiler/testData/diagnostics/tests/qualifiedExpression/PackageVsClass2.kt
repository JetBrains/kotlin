// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1
// FILE: a.kt
package a

class a {
    companion object {}
}

class b {
    fun a_b() {}
}

// MODULE: m2(m1)
// FILE: b.kt
package test1

class a

val x = a.<!UNRESOLVED_REFERENCE!>b<!>()

// FILE: c.kt
package test2

import a.a

fun foo(i: a) {
    a
    a()
    a.<!UNRESOLVED_REFERENCE!>a<!>
    a.<!UNRESOLVED_REFERENCE!>a<!>()
}
