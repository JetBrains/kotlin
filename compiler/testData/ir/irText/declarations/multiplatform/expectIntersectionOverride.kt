// FIR_IDENTICAL
// IGNORE_BACKEND_K1: JVM_IR, JS_IR
// !LANGUAGE: +MultiPlatformProjects

// KT-61141: NO_ACTUAL_FOR_EXPECT: Expected class 'C' has no actual declaration in module <common> for Native (21,14) in /common.kt
// IGNORE_BACKEND_K1: NATIVE

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

@Suppress("ABSTRACT_MEMBER_NOT_IMPLEMENTED") // Workaround for KT-60390
expect class C() : I1, I2

// MODULE: platform()()(common)
// FILE: platform.kt

actual class C : I1, I2 {
    override fun f() = "OK"

    override val p = 42
}