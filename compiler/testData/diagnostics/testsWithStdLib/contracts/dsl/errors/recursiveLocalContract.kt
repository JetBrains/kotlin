// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun nonLocalCase(block: () -> Unit = {}): Boolean {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    block()
    return true
}

fun topLevelFun() {
    fun case_1(): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(null) implies case_1() }
        return true
    }

    fun case_2(): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(null) implies <!UNRESOLVED_REFERENCE!>case_3<!>() }
        return true
    }

    fun case_3(): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(null) implies case_2() }
        return true
    }

    fun case_4(): Boolean {
        kotlin.contracts.<!CONTRACT_NOT_ALLOWED!>contract<!> { returns(null) implies case_1() }
        return true
    }

    fun case_5(): Boolean {
        kotlin.contracts.<!CONTRACT_NOT_ALLOWED!>contract<!> { returns(null) implies nonLocalCase() }
        return true
    }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, functionDeclaration, lambdaLiteral, localFunction,
nullableType */
