// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface X

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val value: Int) : X

fun <T> test(t: T) where T : X, T : Z = t as? Int

fun box(): String = if (test(Z(42)) != null) "fail" else "OK"
