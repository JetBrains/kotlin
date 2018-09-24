// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 4
 DESCRIPTION: Contract isn't in the first position.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26191
 */

import kotlin.contracts.*

fun case_1(value_1: Int?) {
    println("!")
    contract {
        returns(true) implies (value_1 != null)
    } <!CAST_NEVER_SUCCEEDS!>as<!> ContractBuilder
}

fun case_2(value_1: Int?) {
    100 + 10
    throw Exception(contract {
        returns(true) implies (value_1 != null)
    }.toString())
}

fun case_3(value_1: Int?) {
    for (i in 0..10) {
        println(i)
    }
    return contract {
        returns(true) implies (value_1 != null)
    }
}

fun case_4(value_1: Int?) {
    val f = 10 - 20
    val g = contract {
        returns(true) implies (value_1 != null)
    }
}

fun case_5(number: Int?): Boolean {
    val value_1 = number != null
    contract {
        returns(false) implies (value_1)
    } <!CAST_NEVER_SUCCEEDS!>as<!> ContractBuilder
    return number == null
}
