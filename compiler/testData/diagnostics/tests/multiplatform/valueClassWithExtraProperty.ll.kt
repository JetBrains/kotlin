// RUN_PIPELINE_TILL: BACKEND
// IGNORE_FIR_DIAGNOSTICS
// LANGUAGE: +ContextParameters
// WITH_STDLIB
// LL_FIR_DIVERGENCE
// Extra diagnostic in metadata compilation
// LL_FIR_DIVERGENCE

// MODULE: m1-common
// FILE: common.kt
expect value class Value(val x: Int) {
    context(x: String)
    <!CONTEXTUAL_OVERLOAD_SHADOWED!>val x: Int<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
@JvmInline
actual value class Value(val x: Int) {
    context(x: String)
    <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <!ACTUAL_MISSING!>x<!>: Int<!> get() = 1}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, getter, integerLiteral, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, value */
