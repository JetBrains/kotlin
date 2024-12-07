// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-69476

// MODULE: m1-common
// FILE: common.kt
expect sealed class Base()

class A : Base()
object B : Base()

interface I

fun testCommon(base: Base) {
    if (base is I) {
        val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
            <!USELESS_IS_CHECK!>is A<!> -> 1
            B -> 2
        }
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: Base.kt
actual sealed class Base

// FILE: C.kt

class C : Base()

fun testPlatformGood(base: Base) {
    if (base is I) {
        val x = when (base) { // must be OK
            <!USELESS_IS_CHECK!>is A<!> -> 1
            B -> 2
            <!USELESS_IS_CHECK!>is C<!> -> 3
        }
    }
}

fun testPlatformBad(base: Base) {
    if (base is I) {
        val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
            <!USELESS_IS_CHECK!>is A<!> -> 1
            B -> 2
        }
    }
}
