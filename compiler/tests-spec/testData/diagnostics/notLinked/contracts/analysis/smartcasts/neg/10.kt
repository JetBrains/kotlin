// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 10
 DESCRIPTION: Check the lack of smartcasts after non-null assertions or not-null value assignment in lambdas of contract function with 'unknown' or 'at most once' CallsInPlace effects.
 */

fun case_1(arg: Int?) {
    funWithAtMostOnceCallsInPlace { arg!! }
    arg<!UNSAFE_CALL!>.<!>inc()
}

fun case_2(arg: Int?) {
    funWithUnknownCallsInPlace { arg!! }
    arg<!UNSAFE_CALL!>.<!>inc()
}

fun case_3() {
    val value_1: Boolean?
    funWithAtMostOnceCallsInPlace { value_1 = false }
    <!UNINITIALIZED_VARIABLE!>value_1<!><!UNSAFE_CALL!>.<!>not()
}

fun case_4() {
    val value_1: Boolean?
    funWithUnknownCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = true }
    <!UNINITIALIZED_VARIABLE!>value_1<!><!UNSAFE_CALL!>.<!>not()
}
