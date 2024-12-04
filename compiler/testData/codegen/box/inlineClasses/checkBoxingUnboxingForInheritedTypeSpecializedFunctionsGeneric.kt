// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: Int>(val x: T)

abstract class A<T> {
    var t: T? = null
    final fun foo(): T = t!!
}

class B: A<IC<Int>>()

interface I {
    fun foo(): IC<Int>
}

class B2: A<IC<Int>>(), I


fun box(): String {
    val b = B()
    b.t = IC(10)
    if (b.foo() != IC(10)) return "Fail 1"

    val b2 = B2()
    b2.t = IC(10)
    if (b2.foo() != IC(10)) return "Fail 2"

    val b2i: I = b2
    if (b2i.foo() != IC(10)) return "Fail 3"

    return "OK"
}
