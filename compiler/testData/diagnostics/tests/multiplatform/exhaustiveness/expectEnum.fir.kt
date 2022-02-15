// !DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-20306

// MODULE: m1-common
// FILE: common.kt
expect enum class Base {
    A, B
}

fun testCommon(base: Base) {
    val x = when (base) { // must be an error
        Base.A -> 1
        Base.B -> 2
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: Base.kt
actual enum class Base {
    A, B, C
}

fun testPlatformGood(base: Base) {
    val x = when (base) { // must be OK
        Base.A -> 1
        Base.B -> 2
        Base.C -> 3
    }
}

fun testPlatformBad(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
        Base.A -> 1
        Base.B -> 2
    }
}
