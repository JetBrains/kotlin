// DIAGNOSTICS: -UNUSED_VARIABLE
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

// MODULE: m2()(m1)
// FILE: b.kt

import p.*

fun test() {
    val _a = a
    val _v = v
    a()
    B()

    val inst = A()
    val ia = inst.a
    val iv = inst.v
    inst.a()
    inst.B()
}

// MODULE: m3()(m2)
// FILE: c.kt

import <!UNRESOLVED_REFERENCE!>p<!>.*

fun test3() {
    val _a = <!UNRESOLVED_REFERENCE!>a<!>
    val _v = <!UNRESOLVED_REFERENCE!>v<!>
    <!UNRESOLVED_REFERENCE!>a<!>()
    <!UNRESOLVED_REFERENCE!>B<!>()

    val inst = <!UNRESOLVED_REFERENCE!>A<!>()
    val ia = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>
    val iv = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>v<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>B<!>()
}
