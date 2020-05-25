// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean) {
    while (value_1) {
        funWithExactlyOnceCallsInPlace {
            break
        }
        println("1")
    }

    loop@ for (i in 0..10) {
        funWithExactlyOnceCallsInPlace {
            break@loop
        }
        println("1")
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean) {
    for (i in 0..10) {
        funWithExactlyOnceCallsInPlace {
            continue
        }
        println("1")
    }

    loop@ while (value_1) {
        funWithExactlyOnceCallsInPlace {
            continue@loop
        }
        println("1")
    }
}
