// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
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
