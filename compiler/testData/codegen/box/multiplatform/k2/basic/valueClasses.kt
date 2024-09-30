// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-51969
// WITH_STDLIB

// MODULE: common
// FILE: expect.kt

expect value class ExpectValue(val x: String)

// MODULE: main()()(common)
// FILE: actual.kt

@JvmInline
actual value class ExpectValue actual constructor(actual val x: String)

fun box() = ExpectValue("OK").x
