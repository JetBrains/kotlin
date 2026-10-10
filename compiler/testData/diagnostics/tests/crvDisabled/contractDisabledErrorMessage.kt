// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE_FEATURE_TOGGLED: ReturnValueCheckerIsStable
// DISABLE_NEXT_PHASE_SUGGESTION: fails at FRONTEND when the language feature is disabled

import kotlin.contracts.*

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, lambdaLiteral, nullableType, thisExpression, typeParameter */
