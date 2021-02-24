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
    val _a = <!HIDDEN!>a<!>
    val _v = <!HIDDEN!>v<!>
    <!HIDDEN!>a<!>()
    <!HIDDEN!>B<!>()

    val inst = A()
    val ia = inst.<!HIDDEN!>a<!>
    val iv = inst.<!HIDDEN!>v<!>
    inst.<!HIDDEN!>a<!>()
    inst.<!HIDDEN!>B<!>()
}
