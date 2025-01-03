// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-69632
// MODULE: common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Common<!><!>

<!CONFLICTING_OVERLOADS!>expect fun commonFun(a: Common)<!>
expect var <!REDECLARATION!>commonProp<!>: Common

//MODULE: intermediate()()(common)
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Intermediate<!>
actual typealias <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Common<!> = Intermediate

<!CONFLICTING_OVERLOADS!>expect fun intermediateFun(a: Common, b: Intermediate)<!>
expect var <!REDECLARATION!>intermediateProp<!>: Intermediate

// MODULE: main()()(intermediate)
actual class Intermediate

actual fun commonFun(a: Common) {}
actual var commonProp: Common = null!!

actual fun intermediateFun(a: Common, b: Intermediate) {}
actual var intermediateProp: Intermediate = null!!
