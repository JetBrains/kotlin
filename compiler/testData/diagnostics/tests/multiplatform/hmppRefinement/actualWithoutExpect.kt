// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
<!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExperimentalExpectRefinement<!><!>
expect fun foo()

@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExperimentalExpectRefinement<!>
expect class Foo

// MODULE: main()()(common)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>foo<!>() {}
actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>Foo<!> {}
