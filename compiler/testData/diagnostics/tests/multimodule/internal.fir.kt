// !DIAGNOSTICS: -UNUSED_VARIABLE
// MODULE: m1
// FILE: a.kt

package p

public class A {
    internal val a = A()
    internal var v = A()
    internal fun a() = A()
    internal inner class B
}

internal val a = A()
internal var v = A()
internal fun a() = A()
internal class B

// MODULE: m2(m1)
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
