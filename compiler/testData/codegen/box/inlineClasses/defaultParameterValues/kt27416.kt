// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val i: Int) {
    fun foo(s: String = "OK") = s
}

fun box() = A(42).foo()