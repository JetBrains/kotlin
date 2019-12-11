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
    val _a = <!INAPPLICABLE_CANDIDATE!>a<!>
    val _v = <!INAPPLICABLE_CANDIDATE!>v<!>
    <!INAPPLICABLE_CANDIDATE!>a<!>()
    <!INAPPLICABLE_CANDIDATE!>B<!>()

    val inst = A()
    val ia = inst.<!INAPPLICABLE_CANDIDATE!>a<!>
    val iv = inst.<!INAPPLICABLE_CANDIDATE!>v<!>
    inst.<!INAPPLICABLE_CANDIDATE!>a<!>()
    inst.<!INAPPLICABLE_CANDIDATE!>B<!>()
}