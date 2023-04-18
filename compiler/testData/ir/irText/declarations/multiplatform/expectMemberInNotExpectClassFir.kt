// FIR_IDENTICAL
// IGNORE_BACKEND_K1: JVM_IR, JS_IR
// SKIP_KLIB_TEST
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect open class C1() {
    fun f(): String

    val p: Int
}

class C2 : C1()

// MODULE: platform()()(common)
// FILE: platform.kt

actual open class C1 {
    actual fun f() = "O"

    actual val p = 42
}