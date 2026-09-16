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

fun fallback(): String = "fallback"

fun testTryCatchIgnorable(s: String, sb: StringBuilder) {
    s.myLet {
        try {
            sb.append(it)
        } catch (e: RuntimeException) {
            return
        }
    }
}

fun testTryCatchMixed(s: String, sb: StringBuilder) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        try {
            sb.append(it)
        } catch (e: RuntimeException) {
            fallback()
        }
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, flexibleType, funWithExtensionReceiver, functionDeclaration,
functionalType, inline, javaFunction, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral,
thisExpression, tryExpression, typeParameter */
