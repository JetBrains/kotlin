// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: R.kt
import kotlin.jvm.JvmInline

@JvmInline
value class R(val value: String) {
    companion object {
        inline fun ok() = R("OK")
    }
}

// FILE: test.kt

fun box(): String = R.ok().value


