// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +GenericInlineClassParameter
// FILE: R.kt
import kotlin.jvm.JvmInline

@JvmInline
value class R<T: String>(val value: T) {
    companion object {
        inline fun ok() = R("OK")
    }
}

// FILE: test.kt

fun box(): String = R.ok().value


