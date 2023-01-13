// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

interface Foo {
    fun ok(): String = "OK"
}

fun test(e: Foo) = e.ok()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

interface Bar : Foo

class A : Bar

fun box() = A().ok()
