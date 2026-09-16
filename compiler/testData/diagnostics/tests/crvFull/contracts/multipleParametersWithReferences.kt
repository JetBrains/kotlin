// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T, R> choose(
    value: T,
    flag: Boolean,
    onTrue: (T) -> R,
    onFalse: (T) -> R
): R {
    contract {
        callsInPlace(onTrue, InvocationKind.AT_MOST_ONCE)
        returnsResultOf(onTrue)
        callsInPlace(onFalse, InvocationKind.AT_MOST_ONCE)
        returnsResultOf(onFalse)
    }
    return if (flag) onTrue(value) else onFalse(value)
}

@IgnorableReturnValue
fun ignorableOp(s: String): String = s

fun nonIgnorableOp(s: String): String = s + "!"

fun testBothReferencesIgnorable(s: String) {
    choose(s, true, ::ignorableOp, ::ignorableOp)
}

fun testMixedReferences(s: String) {
    <!RETURN_VALUE_NOT_USED!>choose<!>(s, true, ::ignorableOp, ::nonIgnorableOp)
    <!RETURN_VALUE_NOT_USED!>choose<!>(s, false, ::nonIgnorableOp, ::ignorableOp)
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, contractCallsEffect, contracts, functionDeclaration,
functionalType, ifExpression, inline, lambdaLiteral, nullableType, stringLiteral, typeParameter */
