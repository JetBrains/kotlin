// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, initialization
 NUMBER: 4
 DESCRIPTION: CallsInPlace contract functions with name shadowing
 */

fun case_1() {
    val <!UNUSED_VARIABLE!>value_1<!>: Int
    funWithExactlyOnceCallsInPlace {
        val <!NAME_SHADOWING!>value_1<!> = 10
        value_1.inc()
    }
}

fun case_2() {
    val <!UNUSED_VARIABLE!>value_1<!>: Int
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
}

fun case_3() {
    val value_1: Int
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
    funWithExactlyOnceCallsInPlace {
        value_1 = 10
    }
    value_1.inc()
}

fun case_4() {
    val value_1: Int
    funWithAtMostOnceCallsInPlace {
        val <!NAME_SHADOWING!>value_1<!>: Int
        funWithExactlyOnceCallsInPlace {
            value_1 = 10
        }
        funWithUnknownCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    funWithExactlyOnceCallsInPlace {
        value_1 = 10
    }
    value_1.inc()
}

fun case_5() {
    val value_1: Int
    funWithUnknownCallsInPlace {
        val <!NAME_SHADOWING!>value_1<!>: Int
        funWithExactlyOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtMostOnceCallsInPlace {
            value_1.inc()
        }
    }
    funWithExactlyOnceCallsInPlace {
        value_1 = 10
    }
    value_1.inc()
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
    funWithAtLeastOnceCallsInPlace { value_1 = 1 }
    value_1.dec()
}

fun case_7() {
    val value_1: Int
    funWithAtLeastOnceCallsInPlace {
        var <!NAME_SHADOWING!>value_1<!>: Int
        funWithAtLeastOnceCallsInPlace { value_1 = 10 }
        funWithUnknownCallsInPlace { value_1.inc() }
        value_1.inc()
    }
    funWithExactlyOnceCallsInPlace { value_1 = 1 }
    value_1.dec()
}

fun case_8() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace {
        var <!NAME_SHADOWING!>value_1<!>: Int
        funWithAtLeastOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtLeastOnceCallsInPlace {
            value_1.inc()
        }
        value_1++
    }
    funWithAtLeastOnceCallsInPlace {
        value_1 = 1
    }
    value_1--
}


