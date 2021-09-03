// !DIAGNOSTICS: -UNUSED_VARIABLE
// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Int?) {
    println("!")
    contract {
        returns(true) implies (value_1 != null)
    } as ContractBuilder
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int?) {
    100 + 10
    throw Exception(contract {
        returns(true) implies (value_1 != null)
    }.toString())
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Int?) {
    for (i in 0..10) {
        println(i)
    }
    return contract {
        returns(true) implies (value_1 != null)
    }
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Int?) {
    val f = 10 - 20
    val g = contract {
        returns(true) implies (value_1 != null)
    }
}

// TESTCASE NUMBER: 5
fun case_5(number: Int?): Boolean {
    val value_1 = number != null
    contract {
        returns(false) implies (value_1)
    } as ContractBuilder
    return number == null
}
