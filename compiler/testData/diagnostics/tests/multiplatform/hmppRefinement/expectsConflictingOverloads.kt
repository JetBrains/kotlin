// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
<!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{JVM}!>expect fun foo()<!>
<!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{JVM}!>expect fun foo()<!>

// MODULE: main()()(common)
actual fun <!AMBIGUOUS_EXPECTS!>foo<!>() {}

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration */
