import pack.oldFun1
import pack.oldFun2 // should not be removed for non-deprecated overload used
import pack.oldFun3
import kotlin.reflect.KProperty

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
}

fun unnecessarySafeCall(x: String) {
    x?.length
}

fun unnecessaryExclExcl(x: String) {
    x!!.length
}

fun unnecessaryCast(x: String) = x as String

fun unnecessaryElvis(x: String) = x ?: ""

@JavaAnn(1, "abc") class MyClass

val i = 1

annotation class Fancy(val param: Int)

@Fancy(<caret>i) class D

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

    fun willBeInfix(i: Int) {}
}

fun <T> typed() {
}

fun <T : Cloneable> withTypeParameters() where T : Comparable<T> {
}

val x = C() willBeInfix 1

fun infixTest() {
    arrayListOf(1, 2, 3) map { it }
}


fun bar(yield: Int = 4) {}

fun yield(yield: Int) {
    "$yield"
    "${yield}"

    yield
    val foo = yield + yield
    val foo2 = yield

    bar(yield = 5)

    yield(4)
    yield {}

    class yield<T: yield<T>>

    return@yield
    return@yield Unit

    val foo5: yield<*>
}

fun yield(i: (Int) -> Unit) {}

annotation class yield

@yield()
fun test2(p: Int) {
    yield@ p

    @yield fun f() {}
}

object X {
    fun yield() {}

    fun test3(yield: Int) {
        X::yield

        yield::toInt
    }
}

header class Expected

impl class Actual
