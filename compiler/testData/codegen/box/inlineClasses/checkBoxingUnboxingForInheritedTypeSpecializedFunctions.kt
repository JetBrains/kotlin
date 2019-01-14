// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

abstract class A<T> {
    var t: T? = null
    final fun foo(): T = t!!
}

class B: A<Char>()

interface I {
    fun foo(): Char
}

class B2: A<Char>(), I


fun box(): String {
    val b = B()
    b.t = 'c'
    if (b.t != 'c') return "Fail 1"

    val b2 = B2()
    b2.t = 'c'
    if (b2.foo() != 'c') return "Fail 2"

    val b2i: I = b2
    if (b2i.foo() != 'c') return "Fail 3"

    return "OK"
}
