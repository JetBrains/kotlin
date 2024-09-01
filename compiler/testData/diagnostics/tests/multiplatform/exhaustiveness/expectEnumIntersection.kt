// DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-69476

// MODULE: m1-common
// FILE: common.kt
expect enum class Base {
    A, B
}

interface I

fun testCommon(base: Base) {
    if (base is <!INCOMPATIBLE_ENUM_COMPARISON_ERROR, INCOMPATIBLE_ENUM_COMPARISON_ERROR{JVM}!>I<!>) {
        val x = <!EXPECT_TYPE_IN_WHEN_WITHOUT_ELSE("enum"), NO_ELSE_IN_WHEN("'else' branch"), NO_ELSE_IN_WHEN{JVM}("'C' branch or 'else' branch instead")!>when<!> (base) { // must be an error
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
    if (base is <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>I<!>) {
        val x = when (base) { // must be OK
            Base.A -> 1
            Base.B -> 2
            Base.C -> 3
        }
    }
}

fun testPlatformBad(base: Base) {
    if (base is <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>I<!>) {
        val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
            Base.A -> 1
            Base.B -> 2
        }
    }
}
