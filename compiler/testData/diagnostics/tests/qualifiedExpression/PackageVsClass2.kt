// MODULE: m1
// FILE: a.kt
package a

class b {
    fun a_b() {}
}

// MODULE: m2(m1)
// FILE: b.kt
package test

class a

val x = a.<!UNRESOLVED_REFERENCE!>b<!>()