// !DIAGNOSTICS: -UNUSED_VARIABLE
// MODULE: m1
// FILE: a.kt

package p

public class A {
    val a = A()
    var v = A()
    fun a() = A()
    inner class B
}

val a = A()
var v = A()
fun a() = A()
class B

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