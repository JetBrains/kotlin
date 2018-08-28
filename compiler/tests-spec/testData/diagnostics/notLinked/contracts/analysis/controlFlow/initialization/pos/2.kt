// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, initialization
 NUMBER: 2
 DESCRIPTION: Nested val/var assignments using contract functions with CallsInPlace effect
 */

fun case_1() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace {
        funWithExactlyOnceCallsInPlace {
            value_1 = 1
            funWithExactlyOnceCallsInPlace {
                value_1.inc()
            }
        }
        funWithExactlyOnceCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    value_1.inc()
}

fun case_2() {
    val value_1: Int
    funWithAtMostOnceCallsInPlace {
        funWithExactlyOnceCallsInPlace {
            value_1 = 1
        }
        funWithAtLeastOnceCallsInPlace {
            value_1.inc()
        }
        funWithUnknownCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
}

fun case_3() {
    var value_1: Int
    funWithExactlyOnceCallsInPlace {
        funWithExactlyOnceCallsInPlace {
            value_1 = 1
            funWithExactlyOnceCallsInPlace {
                value_1.inc()
            }
        }
        funWithExactlyOnceCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    value_1.inc()
}

fun case_4() {
    var value_1: Int
    funWithAtMostOnceCallsInPlace {
        funWithAtLeastOnceCallsInPlace {
            value_1 = 1
        }
        funWithAtLeastOnceCallsInPlace {
            value_1.inc()
        }
        funWithUnknownCallsInPlace {
            value_1.inc()
        }
        funWithExactlyOnceCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
}

fun case_7() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace {
        funWithAtLeastOnceCallsInPlace {
            value_1 = 1
            funWithAtMostOnceCallsInPlace {
                value_1.inc()
            }
        }
        funWithUnknownCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    value_1.inc()
}

fun case_8() {
    var value_1: Int
    funWithUnknownCallsInPlace {
        funWithAtMostOnceCallsInPlace {
            funWithAtLeastOnceCallsInPlace {
                value_1 = 1
            }
            funWithExactlyOnceCallsInPlace {
                value_1.inc()
            }
            funWithAtLeastOnceCallsInPlace {
                value_1.inc()
            }
            funWithAtMostOnceCallsInPlace {
                value_1.inc()
            }
            funWithUnknownCallsInPlace {
                value_1.inc()
            }
        }
    }
}

fun case_9() {
    var value_1: Int
    funWithAtMostOnceCallsInPlace {
        funWithUnknownCallsInPlace {
            funWithExactlyOnceCallsInPlace {
                value_1 = 1
            }
            funWithAtLeastOnceCallsInPlace {
                value_1.inc()
            }
            funWithAtMostOnceCallsInPlace {
                value_1.inc()
            }
            funWithUnknownCallsInPlace {
                value_1.inc()
            }
        }
    }
}

