// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 1
 * DESCRIPTION: Contract isn't first statement.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    val value_1 = 1
    <!CONTRACT_NOT_ALLOWED!>contract<!> { }
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    10 - 1
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// TESTCASE NUMBER: 3
inline fun case_3(block: () -> Unit) {
    throw Exception()
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
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
    return <!CONTRACT_NOT_ALLOWED!>contract<!> {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    }
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-26191
 */
fun case_5(value_1: Int?) {
    println("!")
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        returns(true) implies (value_1 != null)
    } <!CAST_NEVER_SUCCEEDS!>as<!> ContractBuilder
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-26191
 */
fun case_6(value_1: Int?) {
    100 + 10
    throw Exception(<!CONTRACT_NOT_ALLOWED!>contract<!> {
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
    return <!CONTRACT_NOT_ALLOWED!>contract<!> {
        returns(true) implies (value_1 != null)
    }
}

/*
 * TESTCASE NUMBER: 8
 * ISSUES: KT-26191
 */
fun case_8(value_1: Int?) {
    val f = 10 - 20
    val g = <!CONTRACT_NOT_ALLOWED!>contract<!> {
        returns(true) implies (value_1 != null)
    }
}

/*
 * TESTCASE NUMBER: 9
 * ISSUES: KT-26191
 */
fun case_9(number: Int?): Boolean {
    val value_1 = number != null
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        returns(false) implies (value_1)
    } <!CAST_NEVER_SUCCEEDS!>as<!> ContractBuilder
    return number == null
}
