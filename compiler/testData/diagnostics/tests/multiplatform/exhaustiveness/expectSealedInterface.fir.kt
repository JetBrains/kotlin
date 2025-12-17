// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-44474

// MODULE: m1-common
// FILE: common.kt
expect sealed interface Base

class A : Base
object B : Base

fun testCommon(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
        is A -> 1
        B -> 2
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: Base.kt
actual sealed interface Base

// FILE: C.kt

interface C : Base

fun testPlatformGood(base: Base) {
    val x = <!WHEN_ON_SEALED_GEEN_ELSE!>when (base) { // must be OK
        is A -> 1
        B -> 2
        is C -> 3
    }<!>
}

fun testPlatformBad(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
        is A -> 1
        B -> 2
    }
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, equalityExpression, expect, functionDeclaration, integerLiteral,
interfaceDeclaration, isExpression, localProperty, objectDeclaration, propertyDeclaration, sealed, smartcast,
whenExpression, whenWithSubject */
