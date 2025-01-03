// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>A<!><!> {
    fun foo(x: String): String
}

// MODULE: intermediate()()(common)
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>

actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!><!> {
    actual fun foo(x: B) = "a"
}

// MODULE: main()()(intermediate)
actual typealias B = String
