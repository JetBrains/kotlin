// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: Box.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String) {
    companion object {
        fun unwrap(s: StringWrapper): String = s.s
    }
}

fun box(): String {
    return StringWrapper.unwrap(StringWrapper("OK"))
}