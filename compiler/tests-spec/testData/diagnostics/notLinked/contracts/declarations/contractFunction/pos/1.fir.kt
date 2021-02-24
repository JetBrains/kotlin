// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1_1(x: Any?) {
    if (case_1_2(x)) {
        x.length
    }
}

// TESTCASE NUMBER: 2
fun case_2_1(x: Number?) {
    case_2_2(x)
    println(x.toByte())
}

// TESTCASE NUMBER: 1
fun case_1_2(x: Any?): Boolean {
    contract { returns(true) implies (x is String) }
    return x is String
}

// TESTCASE NUMBER: 2
fun case_2_2(x: Any?) {
    contract { returns() implies(x != null) }
    if (x == null) throw Exception()
}
