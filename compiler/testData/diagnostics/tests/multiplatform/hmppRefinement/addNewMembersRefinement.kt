// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!> {
    fun foo()
}

// MODULE: intermediate1()()(common)
@kotlin.<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE{JVM}!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED, DEBUG_INFO_MISSING_UNRESOLVED{JVM}!>ExperimentalExpectRefinement<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!> {
    fun foo()
    fun bar()
}

// MODULE: intermediate2()()(intermediate1)
@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExperimentalExpectRefinement<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!> {
    fun foo()
    fun bar()
    fun baz()
}

// MODULE: main()()(intermediate2)
actual class <!AMBIGUOUS_EXPECTS, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    actual fun foo() {}
    actual fun bar() {}
    actual fun baz() {}
}
