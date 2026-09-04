// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect class A(val x: Int) {
    fun foo(): Int = this.x
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class A actual constructor(actual val x: Int) {
    actual fun foo(): Int = this.x + 1
}

fun box(): String = if (A(1).foo() == 2) "OK" else "FAIL"
