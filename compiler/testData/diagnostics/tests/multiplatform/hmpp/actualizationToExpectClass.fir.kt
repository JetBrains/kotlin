// ISSUE: KT-69632
// MODULE: common
expect class Common

<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect fun commonFun(a: Common)<!>
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect var commonProp: Common<!>

//MODULE: intermediate()()(common)
expect class Intermediate
actual typealias Common = Intermediate

expect fun intermediateFun(a: Common, b: Intermediate)
expect var intermediateProp: Intermediate

// MODULE: main()()(intermediate)
actual class Intermediate

actual fun commonFun(a: Common) {}
actual var commonProp: Common = null!!

actual fun intermediateFun(a: Common, b: Intermediate) {}
actual var intermediateProp: Intermediate = null!!
