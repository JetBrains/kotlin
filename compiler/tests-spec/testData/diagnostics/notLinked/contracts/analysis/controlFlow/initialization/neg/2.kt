// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, initialization
 NUMBER: 2
 DESCRIPTION: val/var reassignment and/or uninitialized variable usages based on nested CallsInPlace effects with wrong invocation kind
 */

fun case_1() {
    val value_1: Int
    funWithAtLeastOnceCallsInPlace {
        funWithAtMostOnceCallsInPlace {
            <!VAL_REASSIGNMENT!>value_1<!> = 1
            funWithExactlyOnceCallsInPlace {
                value_1.inc()
            }
        }
        funWithExactlyOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        value_1.inc()
    }
    value_1.inc()
}

fun case_2() {
    val value_1: Int
    funWithAtMostOnceCallsInPlace {
        funWithAtMostOnceCallsInPlace {
            value_1 = 1
        }
        funWithAtLeastOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        funWithUnknownCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    value_1.inc()
}

fun case_3() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace {
        funWithAtMostOnceCallsInPlace {
            value_1 = 1
            funWithExactlyOnceCallsInPlace {
                value_1.inc()
            }
        }
        funWithExactlyOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        value_1.inc()
    }
    value_1.inc()
}

fun case_4() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace {
        funWithUnknownCallsInPlace {
            value_1 = 1
        }
        funWithAtLeastOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        funWithUnknownCallsInPlace {
            value_1.inc()
        }
        funWithExactlyOnceCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    value_1.inc()
}
