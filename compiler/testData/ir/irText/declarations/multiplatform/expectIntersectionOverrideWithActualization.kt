// FIR_IDENTICAL
// IGNORE_BACKEND_K1: JVM_IR, JS_IR
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

interface I1 {
    fun f(): String

    val p: Int
}

interface I2 {
    fun f(): String

    val p: Int
}

expect class C() : I1, I2 {
    override fun f(): String
    override val p: Int
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class C : I1, I2 {
    actual override fun f() = "OK"

    actual override val p = 42
}