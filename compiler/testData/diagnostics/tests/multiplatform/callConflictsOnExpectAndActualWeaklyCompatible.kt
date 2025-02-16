// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-61732
// based on of kotlin.text.startsWith from kotlin-stdlib

// MODULE: common
// FILE: common.kt

expect fun String.foo(prefix: String, ignoreCase: Boolean = false): Boolean

expect fun String.foo(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean

// MODULE: jvm()()(common)
// FILE: jvm.kt


@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
actual fun String.foo(prefix: String, ignoreCase: Boolean = false): Boolean {
    return true
}

@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
actual fun String.foo(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean {
    return true
}

// MODULE: client(jvm)()()
// FILE: client.kt

fun main() {
    "".foo("")
}
