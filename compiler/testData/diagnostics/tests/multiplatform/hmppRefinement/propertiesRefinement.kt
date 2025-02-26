// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect internal val <!REDECLARATION{JVM}, REDECLARATION{JVM}, REDECLARATION{JVM}!>foo<!>: Int

// MODULE: intermediate1()()(common)
<!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE{JVM}!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED, DEBUG_INFO_MISSING_UNRESOLVED{JVM}!>ExperimentalExpectRefinement<!><!>
expect internal val <!REDECLARATION, REDECLARATION{JVM}, REDECLARATION{JVM}!>foo<!>: Int

// MODULE: intermediate2()()(intermediate1)
<!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExperimentalExpectRefinement<!><!>
expect public val <!REDECLARATION, REDECLARATION{JVM}!>foo<!>: Int

// MODULE: main()()(intermediate2)
actual public val <!AMBIGUOUS_EXPECTS!>foo<!>: Int = 1
