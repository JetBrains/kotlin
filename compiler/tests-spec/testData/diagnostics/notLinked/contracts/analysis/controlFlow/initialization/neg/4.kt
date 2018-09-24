// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, initialization
 NUMBER: 4
 DESCRIPTION: CallsInPlace contract functions with name shadowing and wrong invocation kind
 */

fun case_1() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace {
        val <!NAME_SHADOWING!>value_1<!> = 10
        value_1.inc()
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

fun case_2() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace {
        val <!NAME_SHADOWING!>value_1<!>: Int
        funWithExactlyOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtLeastOnceCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

fun case_3() {
    val value_1: Int
    funWithAtLeastOnceCallsInPlace {
        val <!NAME_SHADOWING!>value_1<!>: Int
        funWithAtMostOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtMostOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        value_1.inc()
    }
    funWithAtMostOnceCallsInPlace {
        value_1 = 10
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

fun case_6() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace {
        val <!NAME_SHADOWING!>value_1<!>: Int
        funWithExactlyOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtMostOnceCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    funWithAtMostOnceCallsInPlace {
        value_1 = 1
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}

fun case_7() {
    val value_1: Int
    funWithUnknownCallsInPlace {
        var <!NAME_SHADOWING!>value_1<!>: Int
        funWithAtLeastOnceCallsInPlace {
            value_1 = 10
        }
        funWithUnknownCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    funWithUnknownCallsInPlace {
        <!VAL_REASSIGNMENT!>value_1<!> = 1
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}
