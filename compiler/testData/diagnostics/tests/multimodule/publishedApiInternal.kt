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

inline fun testInline() {
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