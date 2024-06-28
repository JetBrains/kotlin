// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

interface Foo {
    fun ok(): String = "OK"
}

fun test(e: Foo) = e.ok()

// MODULE: platform()()(common)
// FILE: platform.kt

interface Bar : Foo

class A : Bar

fun box() = A().ok()
