// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

<!CONFLICTING_OVERLOADS!>expect fun inMethod(@Ann arg: String)<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>InConstructor<!>(@Ann arg: String)

<!CONFLICTING_OVERLOADS!>expect fun withIncopatibility<!NO_ACTUAL_FOR_EXPECT{JVM}!>(@Ann p1: String, @Ann p2: String)<!><!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inMethod<!>(arg: String) {}

actual class InConstructor <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual constructor(arg: String)<!> {}

actual fun withIncopatibility<!ACTUAL_WITHOUT_EXPECT!>(p1: String)<!> {}
