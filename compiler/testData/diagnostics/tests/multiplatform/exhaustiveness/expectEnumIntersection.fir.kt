// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-69476

// MODULE: m1-common
// FILE: common.kt
expect enum class Base {
    A, B
}

interface I

fun testCommon(base: Base) {
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>base is I<!>) {
        val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
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
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>base is I<!>) {
        val x = when (base) { // must be OK
            Base.A -> 1
            Base.B -> 2
            Base.C -> 3
        }
    }
}

fun testPlatformBad(base: Base) {
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>base is I<!>) {
        val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
            Base.A -> 1
            Base.B -> 2
        }
    }
}

/* GENERATED_FIR_TAGS: actual, enumDeclaration, enumEntry, equalityExpression, expect, functionDeclaration, ifExpression,
integerLiteral, interfaceDeclaration, intersectionType, isExpression, localProperty, propertyDeclaration, smartcast,
whenExpression, whenWithSubject */
