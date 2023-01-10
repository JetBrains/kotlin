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
    val _a = <!INVISIBLE_MEMBER!>a<!>
    val _v = <!INVISIBLE_MEMBER!>v<!>
    <!INVISIBLE_MEMBER!>a<!>()
    <!INVISIBLE_MEMBER!>B<!>()

    val inst = A()
    val ia = inst.<!INVISIBLE_MEMBER!>a<!>
    val iv = inst.<!INVISIBLE_MEMBER!>v<!>
    inst.<!INVISIBLE_MEMBER!>a<!>()
    inst.<!INVISIBLE_MEMBER!>B<!>()
}

