// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 9
 DESCRIPTION: Smartcasts after non-null assertions or not-null value assignment in lambdas of contract function with 'exactly once' or 'at least once' CallsInPlace effects.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26148
 */

fun case_1(arg: Int?) {
    funWithExactlyOnceCallsInPlace { arg!! }
    arg<!UNSAFE_CALL!>.<!>inc()
}

fun case_2(arg: Int?) {
    funWithAtLeastOnceCallsInPlace { arg!! }
    arg<!UNSAFE_CALL!>.<!>inc()
}

fun case_3() {
    val value_1: Boolean?
    funWithExactlyOnceCallsInPlace { value_1 = false }
    value_1<!UNSAFE_CALL!>.<!>not()
}

fun case_4() {
    val value_1: Boolean?
    funWithAtLeastOnceCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = true }
    value_1<!UNSAFE_CALL!>.<!>not()
}
