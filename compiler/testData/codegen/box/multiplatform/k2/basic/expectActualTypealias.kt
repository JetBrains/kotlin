// LANGUAGE: +MultiPlatformProjects
// JVM_ABI_K1_K2_DIFF: KT-63903

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect class S {
    val length: Int
}

expect fun foo(): S

fun test(): S = foo()

// MODULE: platform()()(common)
// FILE: platform.kt

actual typealias S = String

actual fun foo(): S = "OK"

fun box() = test()
