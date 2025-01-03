// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect sealed class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Presence<!>
expect object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Online<!>: Presence
expect object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Offline<!>: Presence


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Presence = P
sealed class P
actual object Online : P()
actual object Offline : P()
