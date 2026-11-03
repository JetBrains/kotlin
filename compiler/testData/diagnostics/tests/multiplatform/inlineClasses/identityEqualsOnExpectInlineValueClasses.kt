// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: CODEGEN
// LATEST_LV_DIFFERENCE
// WITH_STDLIB
// LANGUAGE: -StrictEquals
//  ^^^ KT-88389

// MODULE: m1-common
// FILE: common.kt
expect value class Inline(val x: Int)

fun common(a: Inline, b: Inline, any: Any) {
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>a === b<!>
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>a === any<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
@JvmInline
actual value class Inline actual constructor(val x: Int)

fun platform(a: Inline, b: Inline, any: Any) {
    <!FORBIDDEN_IDENTITY_EQUALS!>a === b<!>
    <!FORBIDDEN_IDENTITY_EQUALS!>a === any<!>
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, equalityExpression, expect, functionDeclaration, primaryConstructor,
propertyDeclaration, value */
