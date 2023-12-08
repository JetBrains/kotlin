// IGNORE_BACKEND_K1: ANY
// ^^^ K1 as well as K1-based test infra do not support "fragment refinement".

// FIR_IDENTICAL
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