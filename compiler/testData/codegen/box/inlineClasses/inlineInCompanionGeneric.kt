// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter
// FILE: R.kt
import kotlin.jvm.JvmInline

OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: String>(val value: T) {
    companion object {
        inline fun ok() = R("OK")
    }
}

// FILE: test.kt

fun box(): String = R.ok().value


