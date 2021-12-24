// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Inner<T: String>(val result: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: Inner<String>>(val inner: T = Inner("OK") as T)

fun box(): String {
    return A<Inner<String>>().inner.result
}
