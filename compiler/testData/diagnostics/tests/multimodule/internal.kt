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