// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts, +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.contract

inline fun <R> holdsInAndImpliesReturn(condition: Boolean, x: Any?, block: () -> R): String? {
    contract {
        condition holdsIn block
        (x is String) implies returnsNotNull()
    }
    return ""
}

fun testHoldInAndImpliesReturn(x: Any?) {
    holdsInAndImpliesReturn(
        (x is String),
        x,
        { x.length }
    )<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: contractHoldsInEffect, contractImpliesReturnEffect, contracts, functionDeclaration,
functionalType, inline, isExpression, lambdaLiteral, nullableType, smartcast, stringLiteral, typeParameter */
