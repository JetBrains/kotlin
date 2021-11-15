// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FILE: R.kt
import kotlin.jvm.JvmInline

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class R(val value: String) {
    companion object {
        inline fun ok() = R("OK")
    }
}

// FILE: test.kt

fun box(): String = R.ok().value


