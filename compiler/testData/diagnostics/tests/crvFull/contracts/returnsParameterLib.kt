// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

// MODULE: lib1
// FILE: Lib.kt

import kotlin.contracts.*

fun <T> id(x: T): T {
    contract {
        returnsParameter(x)
    }
    return x
}

inline fun <T> T.myApply(block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsParameter(this@myApply)
    }
    block()
    return this
}

// MODULE: main(lib1)
// FILE: App.kt

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

// Check that the contract 'survives' across modules (serialized to and read from metadata):
fun main(s: String) {
    id(<!RETURN_VALUE_NOT_USED!>fooS<!>())
    id(ign())

    <!RETURN_VALUE_NOT_USED!>fooS<!>().myApply { }
    s.myApply { }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, lambdaLiteral, nullableType, stringLiteral, thisExpression, typeParameter, typeWithExtension */
