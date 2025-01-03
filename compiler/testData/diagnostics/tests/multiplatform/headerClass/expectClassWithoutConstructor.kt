// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Bar<!>()
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Baz<!> constructor()
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>FooBar<!> {
    constructor()
}

<!CONFLICTING_OVERLOADS!>fun test()<!> {
    <!RESOLUTION_TO_CLASSIFIER!>Foo<!>()
    Bar()
    Baz()
    FooBar()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo
actual class Bar
actual class Baz
actual class FooBar
