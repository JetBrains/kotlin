// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Inner(val result: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val inner: Inner = Inner("OK"))

fun box(): String {
    return A().inner.result
}
