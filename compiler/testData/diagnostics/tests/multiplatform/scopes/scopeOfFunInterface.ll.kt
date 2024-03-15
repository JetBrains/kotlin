// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845
// MODULE: common
// FILE: common.kt
expect interface I

fun interface F : I {
    fun foo()
}

// MODULE: jvm()()(common)
// FILE: main.kt

actual interface I {
    fun bar()
}
