// JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface I {
    @Deprecated("message")
    fun result() = "OK"
}

fun box(): String = object : I {}.result()
