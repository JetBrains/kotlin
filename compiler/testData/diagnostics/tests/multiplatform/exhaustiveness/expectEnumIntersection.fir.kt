// DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-69476

// MODULE: m1-common
// FILE: common.kt
expect enum class Base {
    A, B
}

interface I

fun testCommon(base: Base) {
    if (<!USELESS_IS_CHECK, USELESS_IS_CHECK{METADATA}!>base is I<!>) {
        val x = <!NO_ELSE_IN_WHEN, NO_ELSE_IN_WHEN{METADATA}!>when<!> (base) { // must be an error
            Base.A -> 1
            Base.B -> 2
        }
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: Base.kt
actual enum class Base {
    A, B, C
}

fun testPlatformGood(base: Base) {
    if (<!USELESS_IS_CHECK!>base is I<!>) {
        val x = when (base) { // must be OK
            Base.A -> 1
            Base.B -> 2
            Base.C -> 3
        }
    }
}

fun testPlatformBad(base: Base) {
    if (<!USELESS_IS_CHECK!>base is I<!>) {
        val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
            Base.A -> 1
            Base.B -> 2
        }
    }
}
