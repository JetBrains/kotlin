// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    val value_1 = 1
    contract { }
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    10 - 1
    contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// TESTCASE NUMBER: 3
inline fun case_3(block: () -> Unit) {
    throw Exception()
    contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.UNKNOWN)
    }
    return block()
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-26191
 */
inline fun case_4(block: () -> Unit) {
    .0009
    return contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    }
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-26191
 */
fun case_5(value_1: Int?) {
    println("!")
    contract {
        returns(true) implies (value_1 != null)
    } as ContractBuilder
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-26191
 */
fun case_6(value_1: Int?) {
    100 + 10
    throw Exception(contract {
        returns(true) implies (value_1 != null)
    }.toString())
}

/*
 * TESTCASE NUMBER: 7
 * ISSUES: KT-26191
 */
fun case_7(value_1: Int?) {
    for (i in 0..10) {
        println(i)
    }
    return contract {
        returns(true) implies (value_1 != null)
    }
}

/*
 * TESTCASE NUMBER: 8
 * ISSUES: KT-26191
 */
fun case_8(value_1: Int?) {
    val f = 10 - 20
    val g = contract {
        returns(true) implies (value_1 != null)
    }
}

/*
 * TESTCASE NUMBER: 9
 * ISSUES: KT-26191
 */
fun case_9(number: Int?): Boolean {
    val value_1 = number != null
    contract {
        returns(false) implies (value_1)
    } as ContractBuilder
    return number == null
}
