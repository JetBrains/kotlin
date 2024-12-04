// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

fun <T> foo(a: IC): T = a.value as T

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val value: String)

fun box(): String {
    return foo<String>(IC("O")) + "K"
}
