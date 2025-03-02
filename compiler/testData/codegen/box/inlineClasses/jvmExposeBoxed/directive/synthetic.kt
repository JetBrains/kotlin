// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ImplicitJvmExposeBoxed

// FILE: IC.kt
@JvmInline
value class StringWrapper(val s: String) {
    @JvmSynthetic
    fun ok(): String = s
}

fun box(): String {
    return StringWrapper("OK").s
}
