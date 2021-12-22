// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface X<T> {
    operator fun plus(n: Int) : T
    fun next(): T = this + 1
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: Int>(val value: T) : X<A<Int>> {
    override operator fun plus(n: Int): A<Int> = A(value + n)
}

fun box(): String {
    val res = A(1).next()
    return if (res.value == 2) "OK" else "FAIL $res"
}
