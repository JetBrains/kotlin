// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
<!CONFLICTING_OVERLOADS{JVM;JVM}!>expect internal fun foo()<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM;JVM}!>Foo<!> {
    internal fun foo()
}

// MODULE: intermediate()()(common)
<!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{JVM}!><!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>
<!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExpectRefinement<!><!>
expect public fun foo()<!>
<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>
@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExpectRefinement<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!> {
    public fun foo()
}

// MODULE: main()()(intermediate)
actual public fun <!AMBIGUOUS_EXPECTS!>foo<!>() {}
actual class <!AMBIGUOUS_EXPECTS, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    actual public fun foo() {}
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration */
