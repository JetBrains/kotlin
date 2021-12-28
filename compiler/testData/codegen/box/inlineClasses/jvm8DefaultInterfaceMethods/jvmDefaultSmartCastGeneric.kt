// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IOk {
    fun ok(): String = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineClass<T: String>(val s: T) : IOk

fun test(x: Any): String {
    return if (x is InlineClass<*>) x.ok() else "FAIL"
}

fun box() = test(InlineClass("Dummy"))
