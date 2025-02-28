// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>
<!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExpectRefinement<!><!>
fun nonExpect() {}

expect class Foo {
    <!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>
    <!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExpectRefinement<!><!>
    fun foo()
}

// MODULE: main()()(common)
actual class Foo {
    actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>foo<!>() {}
}
