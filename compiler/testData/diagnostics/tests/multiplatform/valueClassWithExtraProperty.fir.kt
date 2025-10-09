// RUN_PIPELINE_TILL: BACKEND
// IGNORE_FIR_DIAGNOSTICS
// LANGUAGE: +ContextParameters
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect value class Value(val x: Int) {
    context(x: String)
    val x: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
@JvmInline
actual value class Value(val x: Int) {
    context(x: String)
    val <!ACTUAL_MISSING!>x<!>: Int get() = 1}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, getter, integerLiteral, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, value */
