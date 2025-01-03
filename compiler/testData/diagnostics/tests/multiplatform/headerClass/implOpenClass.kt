// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>

<!CONFLICTING_OVERLOADS!>expect fun getFoo(): Foo<!>

<!CONFLICTING_OVERLOADS!>fun <T : Foo> bar()<!> {} // no "Foo is final" warning should be here

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo

class Bar : Foo()

actual fun getFoo(): Foo = Bar()
