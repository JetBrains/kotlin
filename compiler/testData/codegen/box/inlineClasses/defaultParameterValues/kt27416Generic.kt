// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: Int>(val i: T) {
    fun foo(s: String = "OK") = s
}

fun box() = A(42).foo()