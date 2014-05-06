//FILE:a.kt
package test_visibility

<!PACKAGE_MEMBER_CANNOT_BE_PROTECTED!>protected<!> class ProtectedClass
<!PACKAGE_MEMBER_CANNOT_BE_PROTECTED!>protected<!> trait ProtectedTrait

<!PACKAGE_MEMBER_CANNOT_BE_PROTECTED!>protected<!> val protected_val : Int = 4
<!PACKAGE_MEMBER_CANNOT_BE_PROTECTED!>protected<!> fun protected_fun() {}

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
    private fun f(<!UNUSED_PARAMETER!>i<!>: Int): B = B()

    fun test() {
        doSmth(i)
    }
}

class B {
    fun bMethod() {}
}

fun test3(a: A) {
    a.<!INVISIBLE_MEMBER!>v<!> //todo .bMethod()
    a.<!INVISIBLE_MEMBER!>f<!>(0, <!TOO_MANY_ARGUMENTS!>1<!>) //todo .bMethod()
}

trait T

open class C : T {
    protected var i : Int = 34
    fun test5() {
        doSmth(i)
    }
}

fun test4(c: C) {
    c.<!INVISIBLE_MEMBER!>i<!>++
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
        doSmth(c.i)
    }
}

class G : T {
    fun test8(c: C) {
        doSmth(c.<!INVISIBLE_MEMBER!>i<!>)
    }
}

fun doSmth(i: Int) = i

//FILE:b.kt
package test_visibility2

import test_visibility.*

fun test() {
    internal_fun()
    <!INVISIBLE_MEMBER!>private_fun<!>()
}