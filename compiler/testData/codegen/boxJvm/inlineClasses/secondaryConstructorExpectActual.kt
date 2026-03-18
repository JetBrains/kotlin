// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// MODULE: common
// FILE: expect.kt

expect value class ExpectValue(val x: String) {
    constructor(x: Int)
}

// MODULE: main()()(common)
// FILE: actual.kt

@JvmInline
actual value class ExpectValue actual constructor(actual val x: String) {
    actual constructor(x: Int) : this(if (x == 42) "OK" else "Not OK: $x")
}

fun box() = ExpectValue(42).x
