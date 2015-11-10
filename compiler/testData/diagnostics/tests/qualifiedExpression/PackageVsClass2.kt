// !DIAGNOSTICS: -UNUSED_PARAMETER
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
package test

class a

val x = a.<!UNRESOLVED_REFERENCE!>b<!>()

// MODULE: m3(m1)
// FILE: c.kt
package test

import a.a

fun foo(i: a) {
    a
    a()
    a.<!UNRESOLVED_REFERENCE!>a<!>
    a.<!UNRESOLVED_REFERENCE!>a<!>()
}
