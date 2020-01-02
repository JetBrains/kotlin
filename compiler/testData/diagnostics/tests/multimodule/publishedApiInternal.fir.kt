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

inline fun testInline() {
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