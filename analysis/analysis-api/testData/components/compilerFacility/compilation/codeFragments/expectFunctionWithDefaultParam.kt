// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt


expect fun foo(default: Int = 42): Int

expect fun bar(): Int

fun test() {
    <caret_context>val x = 0
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: jvm.kt
actual fun foo(default: Int) = default

actual fun bar() = 0

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: common
// ANALYSIS_CONTEXT_MODULE: jvm

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
foo() + bar()