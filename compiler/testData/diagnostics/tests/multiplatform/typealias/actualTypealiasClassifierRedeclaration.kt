// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class <!AMBIGUOUS_ACTUALS{JVM}, NO_ACTUAL_FOR_EXPECT, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>A<!> {}

// MODULE: jvm()()(common)
// FILE: main.kt
actual typealias <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> = B

class B {}

class <!ACTUAL_MISSING, PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {}
