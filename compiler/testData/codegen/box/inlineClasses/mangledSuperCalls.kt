// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class I(val i: Int)

abstract class A {
    abstract fun f(i: I): String
}

open class B : A() {
    override fun f(i: I): String = "OK"
}

class C : B() {
    override fun f(i: I): String = super.f(i)
}

fun box(): String {
    return C().f(I(0))
}
