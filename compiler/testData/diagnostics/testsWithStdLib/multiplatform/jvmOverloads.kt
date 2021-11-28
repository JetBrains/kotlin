// FIR_IDENTICAL
// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect fun foo(x: String, y: Int = -1)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@JvmOverloads
actual fun foo(x: String, y: Int) {}
