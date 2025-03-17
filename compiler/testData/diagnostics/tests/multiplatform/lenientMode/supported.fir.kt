// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// LENIENT_MODE

// MODULE: common
// FILE: common.kt
expect fun foo()
expect val bar: String
expect var baz: Int

expect interface I
expect class C {
    fun foo()
    val bar: String
}
expect object O

// MODULE: jvm()()(common)
// FILE: jvm.kt
fun main() {}