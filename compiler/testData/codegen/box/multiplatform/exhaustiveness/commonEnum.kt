// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-20306

// MODULE: m1-common
// FILE: common.kt
enum class Base {
    A, B
}

fun testCommon(base: Base) {
    val x = when (base) { // must be Ok
        Base.A -> 1
        Base.B -> 2
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: main.kt

fun testPlatform(base: Base) {
    val x = when (base) { // must be OK
        Base.A -> 1
        Base.B -> 2
    }
}

fun box() = "OK"
