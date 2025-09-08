// LANGUAGE: -ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
<!CONFLICTING_OVERLOADS{JVM;JVM}!>expect fun foo()<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM;JVM}!>Foo<!>

// MODULE: common2()()(common1)
<!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{JVM}!>expect fun foo()<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!>

// MODULE: main()()(common2)
actual fun <!AMBIGUOUS_EXPECTS!>foo<!>() {}
actual class <!AMBIGUOUS_EXPECTS, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration */
