// LANGUAGE: +MultiPlatformProjects
// JVM_ABI_K1_K2_DIFF: KT-63903

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect class A<B, C> {
    fun o(b: B): C
}

expect val <D> D.k: D

fun k(): String {
    return "K".k
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class A<C, B> {
    actual fun o(b: C): B = "O" as B
}

actual val <D> D.k: D get() = this as D

fun box() = A<Int, String>().o(42) + k()
