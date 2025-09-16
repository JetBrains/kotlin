// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-26175

import kotlin.contracts.*

inline fun case_1(block: () -> Unit) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>{ callsInPlace(block, InvocationKind.EXACTLY_ONCE) }()<!>
    }
    return block()
}

fun case_2(x: Any?): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true).apply { implies (x is Number) }<!>
    }
    return x is Number
}

fun case_3(x: Any?): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true).also { it implies (x is Number) }<!>
    }
    return x is Number
}

fun case_4(x: Any?): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true).let { it implies (x is Number) }<!>
    }
    return x is Number
}


fun case_5(x: Any?): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true).run { implies (x is Number) }<!>
    }
    return x is Number
}


fun case_6(x: Any?): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true).takeIf { false }<!>
    }
    return x is Number
}

/* GENERATED_FIR_TAGS: contracts, functionDeclaration, functionalType, inline, isExpression, lambdaLiteral, nullableType */
