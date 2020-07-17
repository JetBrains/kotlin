// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call
 * NUMBER: 3
 * DESCRIPTION: "Unsafe" cast doesn't work in case of property infix call
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36786
 */

class B(val memberVal: Any)
class C() {
    infix operator fun invoke(i: Int) { } //(1)
}

// TESTCASE NUMBER: 1
fun case1() {
    val b: B = B(C())
    b <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>memberVal<!> 1           //nok UNRESOLVED_REFERENCE_WRONG_RECEIVER
    b.memberVal.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>invoke<!>(2)   //nok UNRESOLVED_REFERENCE_WRONG_RECEIVER
    b.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>memberVal<!>(1)          //nok UNRESOLVED_REFERENCE_WRONG_RECEIVER

    b.memberVal as C

    b <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>memberVal<!> 1           //nok UNRESOLVED_REFERENCE_WRONG_RECEIVER !!!!
    <!DEBUG_INFO_SMARTCAST!>b.memberVal<!>.invoke(1)   //ok

    b.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>memberVal<!>(1)          //nok UNRESOLVED_REFERENCE_WRONG_RECEIVER  !!!
    <!DEBUG_INFO_SMARTCAST!>(b.memberVal)<!>(1)        //ok

}