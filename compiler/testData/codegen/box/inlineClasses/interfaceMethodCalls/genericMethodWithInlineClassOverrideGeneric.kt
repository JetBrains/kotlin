// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val s: T)

abstract class B<T> {
    abstract fun f(x: T): T
}

class C: B<A<String>>() {
    override fun f(x: A<String>): A<String> = x
}

fun box(): String {
    return C().f(A("OK")).s
}
