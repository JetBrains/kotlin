// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!> : A

// MODULE: m1-jvm()()(m1-common)
actual typealias A = Any
actual class B
