// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, initialization
 NUMBER: 1
 DESCRIPTION: val/var reassignment and/or uninitialized variable usages based on CallsInPlace effect with wrong invocation kind
 */

fun case_1() {
    val value_1: Int
    funWithAtLeastOnceCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = 10 }
    value_1.inc()
}

fun case_2() {
    val value_1: Int
    funWithAtMostOnceCallsInPlace { value_1 = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

fun case_3() {
    val value_1: Int
    funWithUnknownCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

fun case_4() {
    var value_1: Int
    var value_2: Int
    funWithAtMostOnceCallsInPlace { value_1 = 10 }
    funWithUnknownCallsInPlace { value_2 = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
    <!UNINITIALIZED_VARIABLE!>value_2<!>.div(10)
}

class case_5 {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val value_1: Int<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val value_2: Int<!>
    val value_3: Int
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var value_4: Int<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var value_5: Int<!>
    init {
        funWithAtMostOnceCallsInPlace { value_1 = 1 }
        funWithUnknownCallsInPlace { <!VAL_REASSIGNMENT!>value_2<!> = 1 }
        funWithAtLeastOnceCallsInPlace { <!VAL_REASSIGNMENT!>value_3<!> = 1 }
        funWithAtMostOnceCallsInPlace { value_4 = 2 }
        funWithUnknownCallsInPlace { value_5 = 3 }
    }
}

fun case_6() {
    val value_1: Int
    for (i in 0..1)
        funWithExactlyOnceCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}

fun case_7() {
    var value_1: Int
    var i = 0
    while (i < 10) {
        funWithExactlyOnceCallsInPlace { value_1 = 10 }
        i++
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}

fun case_8() {
    var value_1: Int
    if (true) funWithAtLeastOnceCallsInPlace { value_1 = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}
