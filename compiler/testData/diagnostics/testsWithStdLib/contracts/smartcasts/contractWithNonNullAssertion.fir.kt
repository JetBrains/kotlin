// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-26856

import kotlin.contracts.*

fun funWithReturnTrueContract(value: Int?): Boolean? {
    contract {
        returns(true) implies (value != null)
    }
    return value != null
}

fun funWithReturnFalseContract(value: Int?): Boolean {
    contract {
        returns(false) implies (value != null)
    }
    return value != null
}

fun funWithReturnsNotNullContract(value: Int?): Boolean? {
    contract {
        returnsNotNull() implies (value != null)
    }

    return value != null
}


fun case_1(value: Int?) {
    if (funWithReturnTrueContract(value)!!) {
        value.inv()
    }
}

fun case_2(value: Int?) {
    if (!funWithReturnFalseContract(value)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) {
        value.inv()
    }
}

fun case_3(value: Int?) {
    if (funWithReturnsNotNullContract(value)!!) {
        value.inv()
    }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, checkNotNullCall, functionDeclaration,
 ifExpression, nullableType, smartcast */