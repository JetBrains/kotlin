// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class I<T: Int>(val i: T)

abstract class A {
    abstract fun f(i: I<Int>): String
}

open class B : A() {
    override fun f(i: I<Int>): String = "OK"
}

class C : B() {
    override fun f(i: I<Int>): String = super.f(i)
}

fun box(): String {
    return C().f(I(0))
}
