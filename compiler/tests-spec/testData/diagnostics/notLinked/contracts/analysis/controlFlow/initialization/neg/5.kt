// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, initialization
 NUMBER: 5
 DESCRIPTION: CallsInPlace contract functions with invalid lambda passing to function parameter.
 */

fun case_1() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace({ <!CAPTURED_VAL_INITIALIZATION!>value_1<!> = 10 })
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

fun case_2() {
    var value_1: Int
    val l = { value_1 = 10 }
    funWithAtLeastOnceCallsInPlace(l)
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

fun case_3() {
    var value_1: Int
    val l = fun () { value_1 = 10 }
    funWithAtLeastOnceCallsInPlace(l)
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

fun case_4() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace(fun () { value_1 = 10 })
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

fun case_5() {
    val value_1: Int
    val o = object {
        fun l() { <!CAPTURED_VAL_INITIALIZATION!>value_1<!> = 10 }
    }
    funWithExactlyOnceCallsInPlace(o::l)
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}
