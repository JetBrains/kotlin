// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

interface X<T> {
    operator fun plus(n: Int) : T
    fun next(): T = this + 1
}

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC: X<IC>

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val value: Int) : IC() {
    override operator fun plus(n: Int): IC = A(value + n)
}

fun box(): String {
    val res: X<IC> = A(1).next()
    return if ((res as A).value == 2) "OK" else "FAIL $res"
}
