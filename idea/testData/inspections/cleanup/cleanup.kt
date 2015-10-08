import pack.oldFun1
import pack.oldFun2 // should not be removed for non-deprecated overload used
import pack.oldFun3

class A private()

fun foo() {
    @loop
    for (i in 1..100) {
        val v = oldFun2(i as Int) as Int
        /* comment */
        continue@loop
    }

    oldFun1(oldFun2(10))

    oldFun2()

    1.identityEquals(2)
}

fun unnecessarySafeCall(x: String) {
    x?.length()
}

fun unnecessaryExclExcl(x: String) {
    x!!.length()
}

fun unnecessaryCast(x: String) = x as String

fun unnecessaryElvis(x: String) = x ?: ""

@JavaAnn(1, "abc") class MyClass

val i = 1

annotation class Fancy(val param: Int)

@Fancy(<caret>i) class D

class Foo {
    var x: Int = 0
        get = $x
        set(value) { $x = value }
}

class B {
    fun plus(a: A): A = A()
}

fun foo() {
    B() + B()
    B() + B()
    B() + B()
}

class C {
    fun foo() {}

    fun bar() = ::foo
}

fun typed<T>() {
}

fun <T : Cloneable> withTypeParameters() where T : Comparable<T> {
}
