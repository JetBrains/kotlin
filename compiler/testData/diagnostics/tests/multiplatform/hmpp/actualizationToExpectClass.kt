// ISSUE: KT-69632
// MODULE: common
expect class <!NO_ACTUAL_FOR_EXPECT!>Common<!>

expect fun <!NO_ACTUAL_FOR_EXPECT, NO_ACTUAL_FOR_EXPECT{JVM}!>commonFun<!>(a: Common)
expect var <!NO_ACTUAL_FOR_EXPECT, NO_ACTUAL_FOR_EXPECT{JVM}!>commonProp<!>: Common

//MODULE: intermediate()()(common)
expect class <!NO_ACTUAL_FOR_EXPECT!>Intermediate<!>
actual typealias Common = Intermediate

expect fun <!NO_ACTUAL_FOR_EXPECT!>intermediateFun<!>(a: Common, b: Intermediate)
expect var <!NO_ACTUAL_FOR_EXPECT!>intermediateProp<!>: Intermediate

// MODULE: main()()(intermediate)
actual class Intermediate

actual fun commonFun(a: Common) {}
actual var commonProp: Common = null!!

actual fun intermediateFun(a: Common, b: Intermediate) {}
actual var intermediateProp: Intermediate = null!!
