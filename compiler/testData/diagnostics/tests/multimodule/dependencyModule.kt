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
    <!INVISIBLE_MEMBER!>propInternal<!>
    <!INVISIBLE_MEMBER!>propPrivate<!>
    funPublic()
    <!INVISIBLE_MEMBER!>funInternal<!>()
    <!INVISIBLE_MEMBER!>funPrivate<!>()
    ClassPublic()
    <!INVISIBLE_MEMBER!>ClassInternal<!>()
    <!INVISIBLE_MEMBER!>ClassPrivate<!>()

    val inst = A()
    inst.propPublic
    inst.<!INVISIBLE_MEMBER!>propInternal<!>
    inst.<!INVISIBLE_MEMBER!>propPrivate<!>
    inst.funPublic()
    inst.<!INVISIBLE_MEMBER!>funInternal<!>()
    inst.<!INVISIBLE_MEMBER!>funPrivate<!>()
    inst.ClassPublic()
    inst.<!INVISIBLE_MEMBER!>ClassInternal<!>()
    inst.<!INVISIBLE_MEMBER!>ClassPrivate<!>()
}

// MODULE: m3(m2)
// FILE: c.kt

import <!UNRESOLVED_REFERENCE!>p<!>.*

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
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>propPublic<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>propInternal<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>propPrivate<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>funPublic<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>funInternal<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>funPrivate<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ClassPublic<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ClassInternal<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inst<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ClassPrivate<!>()
}
