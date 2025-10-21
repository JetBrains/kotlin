// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74461
// RENDER_DIAGNOSTICS_MESSAGES
// LANGUAGE: +ContextParameters

fun main() {
    val a: (() -> Int) -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val b: Int.(String, Boolean) -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val c: context(Int) Int.(String) -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, localProperty, propertyDeclaration */
