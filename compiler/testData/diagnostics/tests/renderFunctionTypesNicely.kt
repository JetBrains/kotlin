// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74461
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -REDUNDANT_PROJECTION

fun <T> main() {
    val a: (() -> Int) -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val b: Int.(String, Boolean) -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val c: context(Int) Int.(String) -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val d: suspend Int.(String) -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val e: (() -> Int).() -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val f: (() -> Int)?.() -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val g: (T & Any).() -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val h: T.() -> String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    val i: @ExtensionFunctionType kotlin.Function1<in Number, String> = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, localProperty, propertyDeclaration */
