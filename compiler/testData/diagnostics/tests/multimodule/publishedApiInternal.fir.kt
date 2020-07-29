// !DIAGNOSTICS: -UNUSED_VARIABLE -NOTHING_TO_INLINE
// MODULE: m1
// FILE: a.kt

package p

public class A {
    @PublishedApi
    internal val a = A()
    @PublishedApi
    internal var v = A()
    @PublishedApi
    internal fun a() = A()
    @PublishedApi
    internal inner class B
}

@PublishedApi
internal val a = A()
@PublishedApi
internal var v = A()
@PublishedApi
internal fun a() = A()
@PublishedApi
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

inline fun testInline() {
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
