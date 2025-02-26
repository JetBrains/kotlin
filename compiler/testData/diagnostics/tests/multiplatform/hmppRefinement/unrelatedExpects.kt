// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
<!CONFLICTING_OVERLOADS{JVM}!>expect fun foo()<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!>

// MODULE: common2
<!CONFLICTING_OVERLOADS{JVM}!>expect fun foo()<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!>

// MODULE: main()()(common1, common2)
actual fun <!AMBIGUOUS_EXPECTS!>foo<!>() {}
actual class <!AMBIGUOUS_EXPECTS, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>
