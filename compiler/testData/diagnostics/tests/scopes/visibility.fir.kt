// !DIAGNOSTICS: -UNUSED_PARAMETER

//FILE:a.kt
package test_visibility

<!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> class ProtectedClass
<!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> interface ProtectedTrait

<!WRONG_MODIFIER_TARGET!>protected<!> val protected_val : Int = 4
<!WRONG_MODIFIER_TARGET!>protected<!> fun protected_fun() {}

private val private_val : Int = 4
private fun private_fun() {}

val internal_val : Int = 34
fun internal_fun() {}

fun test1() {
    private_fun();
}

class Y {
    fun test2() {
        private_fun();
    }
}

class A {
    private val i = 23
    private val v: B = B()
    private fun f(i: Int): B = B()

    fun test() {
        doSmth(i)
    }
}

class B {
    fun bMethod() {}
}

fun test3(a: A) {
    a.<!INVISIBLE_REFERENCE!>v<!> //todo .bMethod()
    a.<!INVISIBLE_REFERENCE!>f<!>(0, 1) //todo .bMethod()
}

interface T

open class C : T {
    protected var i : Int = 34
    fun test5() {
        doSmth(i)
    }
}

fun test4(c: C) {
    c.<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>i<!>++
}

class D : C() {
    val j = i
    fun test6() {
        doSmth(i)
    }
}

class E : C() {
    fun test7() {
        doSmth(i)
    }
}

class F : C() {
    fun test8(c: C) {
        doSmth(c.<!INVISIBLE_REFERENCE!>i<!>)
    }
}

class G : T {
    fun test8(c: C) {
        doSmth(c.<!INVISIBLE_REFERENCE!>i<!>)
    }
}

fun doSmth(i: Int) = i

//FILE:b.kt
package test_visibility2

import test_visibility.*

fun test() {
    internal_fun()
    <!INVISIBLE_REFERENCE!>private_fun<!>()
}
