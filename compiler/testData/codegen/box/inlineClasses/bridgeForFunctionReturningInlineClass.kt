// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val x: String)

interface I<T> {
    fun foo(): T
}

interface II: I<IC>

class A : I<IC> {
    override fun foo() = IC("O")
}

class B : II {
    override fun foo() = IC("K")
}

fun box() = A().foo().x + B().foo().x
