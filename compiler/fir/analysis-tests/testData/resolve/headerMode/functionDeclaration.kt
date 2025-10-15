// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun funA(): String {
    return "funA body"
}

inline fun funB: String {
    return "funB body"
}

@OptIn(ExperimentalContracts::class)
fun isNotNull(value: Any?): Boolean {
    contract {
        returns(true) implies (value != null)
    }
    return value != null
}

private fun funC(): String {
    return "funC body"
}

fun funD() = 1 + 2

/* GENERATED_FIR_TAGS: classReference, contractConditionalEffect, contracts, functionDeclaration, inline, nullableType,
stringLiteral */
