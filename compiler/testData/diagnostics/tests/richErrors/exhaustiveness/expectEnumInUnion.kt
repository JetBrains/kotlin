// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -UNUSED_VARIABLE
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-20306

// MODULE: m1-common
// FILE: common.kt
expect enum class Base {
    A, B
}

error class Foo

fun testCommon(base: Base | Foo) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
        Base.A -> 1
        Base.B -> 2
        is Foo -> 0
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: Base.kt
actual enum class Base {
    A, B, C
}

fun testPlatformGood(base: Base | Foo) {
    val x = when (base) { // must be OK
        Base.A -> 1
        Base.B -> 2
        Base.C -> 3
        is Foo -> 0
    }
}

fun testPlatformBad(base: Base | Foo) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
        Base.A -> 1
        Base.B -> 2
        is Foo -> 0
    }
}

/* GENERATED_FIR_TAGS: actual, enumDeclaration, enumEntry, equalityExpression, expect, functionDeclaration,
integerLiteral, localProperty, propertyDeclaration, smartcast, whenExpression, whenWithSubject */
