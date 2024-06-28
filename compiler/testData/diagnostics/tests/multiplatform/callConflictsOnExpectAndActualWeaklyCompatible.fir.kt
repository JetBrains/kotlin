// ISSUE: KT-61732
// based on of kotlin.text.startsWith from kotlin-stdlib

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun String.foo(prefix: String, ignoreCase: Boolean = false): Boolean

expect fun String.foo(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: jvm.kt


@Suppress(<!ERROR_SUPPRESSION!>"ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS"<!>)
actual fun String.foo(prefix: String, ignoreCase: Boolean = false): Boolean {
    return true
}

@Suppress(<!ERROR_SUPPRESSION!>"ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS"<!>)
actual fun String.foo(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean {
    return true
}

// MODULE: client(jvm)()()
// TARGET_PLATFORM: JVM
// FILE: client.kt

fun main() {
    "".foo("")
}
