// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

fun <T> foo(a: IC<String>): T = a.value as T

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: String>(val value: T)

fun box(): String {
    return foo<String>(IC("O")) + "K"
}
