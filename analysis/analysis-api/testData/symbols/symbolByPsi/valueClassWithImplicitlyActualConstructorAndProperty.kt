// KT-80234
// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1

// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt

expect value class A(val name: String)

expect inline class B(val s: String)

// MODULE: jvm()()(common)
// FILE: main.kt

actual value class A(val name: String)

actual inline class B(val s: String)
