// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-20306

// MODULE: common
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

// MODULE: platform()()(common)
// FILE: main.kt

fun testPlatform(base: Base) {
    val x = when (base) { // must be OK
        Base.A -> 1
        Base.B -> 2
    }
}

fun box() = "OK"
