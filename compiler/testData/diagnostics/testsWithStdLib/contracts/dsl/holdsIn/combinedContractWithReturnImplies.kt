// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.contract

inline fun <R> holdsInAndReturnImplies(condition: Boolean, block: () -> R) {
    contract {
        condition holdsIn block
        returns() implies condition
    }
}

fun testHoldInAndReturnImplies(x: Any?) {
    holdsInAndReturnImplies(x is String) {
        x.length
    }
    x.length
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contractHoldsInEffect, contracts, functionDeclaration, functionalType,
inline, isExpression, lambdaLiteral, nullableType, typeParameter */
