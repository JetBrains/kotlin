// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JVM

OPTIONAL_JVM_INLINE_ANNOTATION
value class Inlined(val value: Int)

sealed interface A <T: Inlined> {
    fun foo(i: T?)
}

class B : A<Nothing> {
    override fun foo(i: Nothing?) {}
}

fun box(): String {
    val a: A<*> = B()
    a.foo(null)
    return "OK"
}
