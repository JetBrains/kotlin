// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-26175

import kotlin.contracts.*

fun case_1(x: Any?) : Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>{ returns(true) implies (x is Number) }()<!>
    }
    return x is Number
}

fun case_2(x: Any?): Boolean {
    contract {
        returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>apply { implies (x is Number) }<!>
    }
    return x is Number
}

fun case_3(x: Any?): Boolean {
    contract {
        returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>also { it implies (x is Number) }<!>
    }
    return x is Number
}

fun case_4(x: Any?): Boolean {
    contract {
        returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>let { it implies (x is Number) }<!>
    }
    return x is Number
}


fun case_5(x: Any?): Boolean {
    contract {
        returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>run { implies (x is Number) }<!>
    }
    return x is Number
}


fun case_6(x: Any?): Boolean {
    contract {
        returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>takeIf { false }<!>
    }
    return x is Number
}

fun case_7(x: Any?): Boolean {
    contract {
        returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>takeIf { it implies (x is Number); false }<!>
    }
    return x is Number
}

/* GENERATED_FIR_TAGS: contracts, functionDeclaration, functionalType, inline, isExpression, lambdaLiteral, nullableType */
