// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
<!CONFLICTING_OVERLOADS{JVM}, CONFLICTING_OVERLOADS{JVM}!>expect fun foo()<!>

// MODULE: common2()()(common1)
<!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{JVM}!>expect fun foo()<!>

// MODULE: main()()(common2)
actual fun <!AMBIGUOUS_EXPECTS!>foo<!>() {}
