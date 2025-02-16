// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-69632
// MODULE: common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class Common<!>

expect fun commonFun(a: Common)
expect var commonProp: Common

//MODULE: intermediate()()(common)
expect class Intermediate
actual typealias <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>Common<!> = Intermediate

expect fun intermediateFun(a: Common, b: Intermediate)
expect var intermediateProp: Intermediate

// MODULE: main()()(intermediate)
actual class Intermediate

actual fun commonFun(a: Common) {}
actual var commonProp: Common = null!!

actual fun intermediateFun(a: Common, b: Intermediate) {}
actual var intermediateProp: Intermediate = null!!
