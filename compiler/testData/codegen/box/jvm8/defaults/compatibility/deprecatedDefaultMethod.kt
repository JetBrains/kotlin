// !JVM_DEFAULT_MODE: compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface I {
    @Deprecated("message")
    @JvmDefault
    fun result() = "OK"
}

fun box(): String = object : I {}.result()
