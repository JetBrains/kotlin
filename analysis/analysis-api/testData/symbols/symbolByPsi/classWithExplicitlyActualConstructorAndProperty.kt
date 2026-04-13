// KT-80234

// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt

expect annotation class A(val name: String)

expect value class B(val name: String)

expect inline class C(val s: String)

// MODULE: jvm()()(common)
// FILE: main.kt

actual annotation class A actual constructor(actual val name: String)

actual value class B actual constructor(actual val name: String)

actual inline class C actual constructor(actual val s: String)
