// WITH_REFLECT
// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

annotation class Ann(val value: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class C<T>(val x: String)

@Ann("OK")
val <T> C<T>.value: String
    get() = x

fun box() = (C<Any?>::value.annotations.singleOrNull() as? Ann)?.value ?: "null"
