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