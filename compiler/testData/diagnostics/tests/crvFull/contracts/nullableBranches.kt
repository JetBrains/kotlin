// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

fun fallback(): String? = "fallback"

fun testNullableBranches(s: String?, sb: StringBuilder) {
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        if (it.isEmpty()) fallback() else null
    }

    s?.myLet {
        if (it.isEmpty()) return@myLet null
        sb.append(it)
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, flexibleType, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, inline, javaFunction, lambdaLiteral, nullableType, safeCall, stringLiteral, thisExpression,
typeParameter */
