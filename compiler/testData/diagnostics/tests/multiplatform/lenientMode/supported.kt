// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// LENIENT_MODE

// MODULE: common
// FILE: common.kt
expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}!>foo<!>()
expect val <!NO_ACTUAL_FOR_EXPECT{JVM}!>bar<!>: String
expect var <!NO_ACTUAL_FOR_EXPECT{JVM}!>baz<!>: Int

expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}!>I<!>
expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>C<!> {
    fun foo()
    val bar: String
}
expect object <!NO_ACTUAL_FOR_EXPECT{JVM}!>O<!>

// MODULE: jvm()()(common)
// FILE: jvm.kt
fun main() {}