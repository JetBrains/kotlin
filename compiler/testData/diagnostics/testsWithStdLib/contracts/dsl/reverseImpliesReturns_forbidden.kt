// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

fun decodeNumberNotInt(encoded: Any): String? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract {
        (encoded is Number && encoded !is Int) implies (returnsNotNull())
    }<!>
    return when (encoded) {
        is Int -> null
        is Number -> encoded.toString()
        else -> null
    }
}

fun decodeStringAndNumButNotInt(encoded: Any): String? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract {
        ((encoded is Number && encoded !is Int) || encoded is String) implies (returnsNotNull())
    }<!>
    return when (encoded) {
        is Int -> null
        is Number -> encoded.toString()
        is String -> encoded
        else -> null
    }
}


enum class E {
    Zero, One
}

fun decodeE(encoded: E): String? {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>(encoded != E.Zero) implies (returnsNotNull())<!>
    }
    return when (encoded) {
        E.One -> "one"
        else -> null
    }
}
