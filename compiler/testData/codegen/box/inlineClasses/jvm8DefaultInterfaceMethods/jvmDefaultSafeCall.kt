// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// JVM_TARGET: 1.8

interface IOk {
    fun ok(): String = "OK"
}

@JvmInline
value class InlineClass(val s: String) : IOk

fun test(x: InlineClass?) = x?.ok() ?: "Failed"

fun box() = test(InlineClass(""))
