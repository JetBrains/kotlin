// JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface IOk {
    fun ok(): String = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineClass(val s: String) : IOk

fun test(x: InlineClass?) = x?.ok() ?: "Failed"

fun box() = test(InlineClass(""))
