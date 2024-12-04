// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface X

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val value: T) : X

fun <T> test(t: T) where T : X, T : Z<Int> = t as? Int

fun box(): String = if (test(Z(42)) != null) "fail" else "OK"
