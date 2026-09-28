// WITH_STDLIB
// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED

// FILE: IC.kt
@JvmInline
value class StringWrapper(val s: String) {
    @JvmSynthetic
    fun ok(): String = s
}

fun box(): String {
    return StringWrapper("OK").s
}
