// ISSUE: KT-51969
// !LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: expect.kt

expect value class ExpectValue(val x: String)

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: actual.kt

@JvmInline
actual value class ExpectValue actual constructor(actual val x: String)

fun box() = ExpectValue("OK").x