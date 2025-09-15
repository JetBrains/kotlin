// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-26856

import kotlin.contracts.*

fun returnsTrueIfNotNullNullable(value: Int?): Boolean? {
    contract {
        returns(true) implies (value != null)
    }
    return value != null
}

fun returnsTrueIfNotNull(value: Int?): Boolean {
    contract {
        returns(true) implies (value != null)
    }
    return value != null
}

fun returnsNotNullIfValueNotNull(value: Int?): Boolean? {
    contract {
        returnsNotNull() implies (value != null)
    }
    return value != null
}


fun case_1(value: Int?) {
    if (returnsTrueIfNotNullNullable(value)!!) {
        value.inv()
    }
}

fun case_2(value: Int?) {
    if (returnsTrueIfNotNull(value)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) {
        value.inv()
    }
}

fun case_3(value: Int?) {
    if (returnsNotNullIfValueNotNull(value)!!) {
        value.inv()
    }
}

fun case_4(value: Int?) {
    returnsNotNullIfValueNotNull(value)!!
    value.inv()
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, checkNotNullCall, functionDeclaration,
 ifExpression, nullableType, smartcast */