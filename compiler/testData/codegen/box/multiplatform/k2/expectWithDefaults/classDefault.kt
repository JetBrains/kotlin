// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect class A(val x: Int) {
    fun foo(): Int = this.x
}

// MODULE: platform()()(common)
// FILE: platform.kt

fun box(): String = if (A(1).foo() == 1) "OK" else "FAIL"
