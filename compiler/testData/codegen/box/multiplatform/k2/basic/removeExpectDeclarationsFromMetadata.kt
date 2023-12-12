// ISSUE: KT-57250
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalMultiplatform
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect class C()

@OptionalExpectation
expect annotation class WithActual(val x: Int)

@OptionalExpectation
expect annotation class WithoutActual(val s: String)

expect fun k(): String

// MODULE: lib()()(common)
// FILE: lib.kt

actual class C {
    fun o() = "O"
}

actual annotation class WithActual(actual val x: Int)

actual fun k() = "K"

// MODULE: common2
// TARGET_PLATFORM: Common
// FILE: common2.kt

@WithoutActual("OK")
fun ok() = C().o() + k()

// MODULE: main(lib)()(common2)
// FILE: main.kt

@WithActual(42)
fun box() = ok()
