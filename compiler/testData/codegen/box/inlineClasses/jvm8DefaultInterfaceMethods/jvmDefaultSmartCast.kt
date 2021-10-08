// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// JVM_TARGET: 1.8

interface IOk {
    fun ok(): String = "OK"
}

@JvmInline
value class InlineClass(val s: String) : IOk

fun test(x: Any): String {
    return if (x is InlineClass) x.ok() else "FAIL"
}

fun box() = test(InlineClass("Dummy"))
