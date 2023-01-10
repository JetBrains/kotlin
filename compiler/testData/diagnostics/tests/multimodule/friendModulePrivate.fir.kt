// !DIAGNOSTICS: -UNUSED_VARIABLE
// MODULE: m1
// FILE: a.kt

package p

public class A {
    private val a = A()
    private var v = A()
    private fun a() = A()
    private inner class B
}

private val a = A()
private var v = A()
private fun a() = A()
private class B

// MODULE: m2()(m1)
// FILE: b.kt

import p.*

fun test() {
    val _a = <!INVISIBLE_REFERENCE!>a<!>
    val _v = <!INVISIBLE_REFERENCE!>v<!>
    <!INVISIBLE_REFERENCE!>a<!>()
    <!INVISIBLE_REFERENCE!>B<!>()

    val inst = A()
    val ia = inst.<!INVISIBLE_REFERENCE!>a<!>
    val iv = inst.<!INVISIBLE_REFERENCE!>v<!>
    inst.<!INVISIBLE_REFERENCE!>a<!>()
    inst.<!INVISIBLE_REFERENCE!>B<!>()
}
