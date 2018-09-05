// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, unreachableCode
 NUMBER: 7
 DESCRIPTION: Smart initialization with correspond contract function with default value before lambda.
 ISSUES: KT-26444
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun case_1(x: Double = 1.0, block: () -> Unit): Double {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }
    return x
}

// FILE: usages.kt

import contracts.*

fun case_1() {
    contracts.case_1 { throw Exception() }
    <!UNREACHABLE_CODE!>println(1)<!>
}
