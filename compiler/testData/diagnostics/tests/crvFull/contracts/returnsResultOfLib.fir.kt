// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

// MODULE: lib1
// FILE: MyLet.kt

import kotlin.contracts.*

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

// MODULE: main(lib1)
// FILE: App.kt

// Check that contract 'survives' across modules:
fun main(s: String?, sb: StringBuilder) {
    s?.myLet { sb.append(it) }
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> { sb.toString() + it }
}

/* GENERATED_FIR_TAGS: additiveExpression, contractCallsEffect, contracts, flexibleType, funWithExtensionReceiver,
functionDeclaration, functionalType, inline, javaFunction, lambdaLiteral, nullableType, safeCall, thisExpression,
typeParameter */
