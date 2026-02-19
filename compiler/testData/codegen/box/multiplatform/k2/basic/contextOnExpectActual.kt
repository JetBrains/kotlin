// LANGUAGE: +MultiPlatformProjects, +ContextParameters

// MODULE: common
// FILE: common.kt

context(a: String)
expect fun foo(): String

context(a: String)
expect val bar: String

// MODULE: platform()()(common)
// FILE: platform.kt

context(a: String)
actual fun foo(): String { return a }

context(a: String)
actual val bar: String
    get() = a

fun box(): String = with("O"){ foo() } + with("K"){ bar }