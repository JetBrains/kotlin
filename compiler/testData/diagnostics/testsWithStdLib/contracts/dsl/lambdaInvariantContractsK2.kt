// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

inline fun <R> runIf(condition: Boolean, block: () -> R): R? {
    contract { condition holdsIn block }
    return if (condition) { block() } else null
}

fun testRunIf(s: Any) {
    val x = runIf(s is String) {
        // smartcast to String
        s.length
        Unit
    }
}

inline fun <R> runIfNot(condition: Boolean, block: () -> R): R? {
    contract {
        !condition holdsIn block
    }
    return if (!condition) block() else null
}

fun testRunIfNot(s: String?) {
    runIfNot(s == null) {
        s.length
    }
}

/* GENERATED_FIR_TAGS: contracts, equalityExpression, functionDeclaration, functionalType, ifExpression, inline,
isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, typeParameter */
