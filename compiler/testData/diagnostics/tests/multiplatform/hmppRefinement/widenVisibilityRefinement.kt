// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
<!CONFLICTING_OVERLOADS{JVM}, CONFLICTING_OVERLOADS{JVM}!>expect internal fun foo()<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!> {
    internal fun foo()
}

// MODULE: intermediate()()(common)
<!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{JVM}!><!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExpectRefinement<!><!>
expect public fun foo()<!>
@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExpectRefinement<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!> {
    public fun foo()
}

// MODULE: main()()(intermediate)
actual public fun <!AMBIGUOUS_EXPECTS!>foo<!>() {}
actual class <!AMBIGUOUS_EXPECTS, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    actual public fun foo() {}
}
