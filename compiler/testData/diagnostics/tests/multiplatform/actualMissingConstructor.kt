// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>()
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo2<!>()

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Bar<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Bar2<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo<!ACTUAL_MISSING!>()<!>
actual class Foo2 {
    <!ACTUAL_MISSING!>constructor()<!>
}

actual class Bar()
actual class Bar2 {
    constructor()
}
