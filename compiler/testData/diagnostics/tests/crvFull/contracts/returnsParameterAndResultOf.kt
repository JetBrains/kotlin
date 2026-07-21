// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// A function that returns EITHER the result of the lambda (returnsResultOf) OR its receiver (returnsParameter),
// depending on the execution path. The call is non-ignorable if either potential return value is non-ignorable.
inline fun <T> T.runOrSelf(useBlock: Boolean, block: (T) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        returnsResultOf(block)
        returnsParameter(this@runOrSelf)
    }
    return if (useBlock) block(this) else this
}

// A value-parameter variant: returns EITHER the lambda result OR the `value` parameter.
inline fun <T> pick(value: T, useBlock: Boolean, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        returnsResultOf(block)
        returnsParameter(value)
    }
    return if (useBlock) block() else value
}

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testReceiverCombined(c: Boolean) {
    // Non-ignorable via both the receiver (returnsParameter) and the lambda result (returnsResultOf).
    <!RETURN_VALUE_NOT_USED!>fooS<!>().<!RETURN_VALUE_NOT_USED!>runOrSelf<!>(c) { fooS() }

    // Non-ignorable only via the receiver (returnsParameter).
    <!RETURN_VALUE_NOT_USED!>fooS<!>().runOrSelf(c) { ign() }

    // Non-ignorable only via the lambda result (returnsResultOf).
    ign().<!RETURN_VALUE_NOT_USED!>runOrSelf<!>(c) { fooS() }

    // Ignorable on both paths => not reported.
    ign().runOrSelf(c) { ign() }

    // Result is used => not reported.
    val x = fooS().runOrSelf(c) { fooS() }
    println(x)
}

fun testValueParameterCombined(c: Boolean) {
    // Non-ignorable via both the value parameter and the lambda result.
    <!RETURN_VALUE_NOT_USED!>pick<!>(<!RETURN_VALUE_NOT_USED!>fooS<!>(), c) { fooS() }

    // Non-ignorable only via the value parameter (returnsParameter).
    pick(<!RETURN_VALUE_NOT_USED!>fooS<!>(), c) { ign() }

    // Non-ignorable only via the lambda result (returnsResultOf).
    <!RETURN_VALUE_NOT_USED!>pick<!>(ign(), c) { fooS() }

    // Ignorable on both paths => not reported.
    pick(ign(), c) { ign() }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, inline, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, thisExpression,
typeParameter */
