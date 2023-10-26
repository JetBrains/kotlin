// !DIAGNOSTICS: -UNUSED_VARIABLE
// MODULE: m1
// FILE: a.kt

package p

public class A {
    public val propPublic = A()
    internal val propInternal = A()
    private val propPrivate = A()
    public fun funPublic() = A()
    internal fun funInternal() = A()
    private fun funPrivate() = A()
    public inner class ClassPublic
    internal inner class ClassInternal
    private inner class ClassPrivate
}

public val propPublic = A()
internal val propInternal = A()
private val propPrivate = A()
public fun funPublic() = A()
internal fun funInternal() = A()
private fun funPrivate() = A()
public class ClassPublic
internal class ClassInternal
private class ClassPrivate

// MODULE: m2(m1)
// FILE: b.kt

import p.*

fun test2() {
    propPublic
    <!INVISIBLE_REFERENCE!>propInternal<!>
    <!INVISIBLE_REFERENCE!>propPrivate<!>
    funPublic()
    <!INVISIBLE_REFERENCE!>funInternal<!>()
    <!INVISIBLE_REFERENCE!>funPrivate<!>()
    ClassPublic()
    <!INVISIBLE_REFERENCE!>ClassInternal<!>()
    <!INVISIBLE_REFERENCE!>ClassPrivate<!>()

    val inst = A()
    inst.propPublic
    inst.<!INVISIBLE_REFERENCE!>propInternal<!>
    inst.<!INVISIBLE_REFERENCE!>propPrivate<!>
    inst.funPublic()
    inst.<!INVISIBLE_REFERENCE!>funInternal<!>()
    inst.<!INVISIBLE_REFERENCE!>funPrivate<!>()
    inst.ClassPublic()
    inst.<!INVISIBLE_REFERENCE!>ClassInternal<!>()
    inst.<!INVISIBLE_REFERENCE!>ClassPrivate<!>()
}

// MODULE: m3(m2)
// FILE: c.kt

import <!UNRESOLVED_IMPORT!>p<!>.*

fun test3() {
    <!UNRESOLVED_REFERENCE!>propPublic<!>
    <!UNRESOLVED_REFERENCE!>propInternal<!>
    <!UNRESOLVED_REFERENCE!>propPrivate<!>
    <!UNRESOLVED_REFERENCE!>funPublic<!>()
    <!UNRESOLVED_REFERENCE!>funInternal<!>()
    <!UNRESOLVED_REFERENCE!>funPrivate<!>()
    <!UNRESOLVED_REFERENCE!>ClassPublic<!>()
    <!UNRESOLVED_REFERENCE!>ClassInternal<!>()
    <!UNRESOLVED_REFERENCE!>ClassPrivate<!>()

    val inst = <!UNRESOLVED_REFERENCE!>A<!>()
    inst.propPublic
    inst.propInternal
    inst.propPrivate
    inst.funPublic()
    inst.funInternal()
    inst.funPrivate()
    inst.ClassPublic()
    inst.ClassInternal()
    inst.ClassPrivate()
}
