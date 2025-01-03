// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {}
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!> : A

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A2<!>() {}
expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B2<!> : <!CYCLIC_INHERITANCE_HIERARCHY{JVM}!>A2<!> {}

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A3<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias <!ACTUAL_WITHOUT_EXPECT!>B<!> = A

actual typealias A2 = B2
actual open class B2 {}

actual typealias A3 = Any
