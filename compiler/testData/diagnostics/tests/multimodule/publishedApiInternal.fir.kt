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

inline fun testInline() {
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
