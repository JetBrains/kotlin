// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Value<T: Any>(val value: T)

fun foo(value: Value<String>?) = value?.value

fun box(): String = (null as Value<String>?).let(::foo) ?: "OK"
