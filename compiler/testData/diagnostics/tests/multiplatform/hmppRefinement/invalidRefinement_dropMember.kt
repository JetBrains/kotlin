// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
expect class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM;JVM}!>Foo<!> {
    fun foo()
}

// MODULE: intermediate()()(common)
<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>
@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExpectRefinement<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!>

// MODULE: main()()(intermediate)
actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, AMBIGUOUS_EXPECTS, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration */
