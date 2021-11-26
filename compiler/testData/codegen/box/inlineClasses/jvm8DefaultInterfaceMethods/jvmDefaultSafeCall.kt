// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8

interface IOk {
    fun ok(): String = "OK"
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlineClass(val s: String) : IOk

fun test(x: InlineClass?) = x?.ok() ?: "Failed"

fun box() = test(InlineClass(""))
