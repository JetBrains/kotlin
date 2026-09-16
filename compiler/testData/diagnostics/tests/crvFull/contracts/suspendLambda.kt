// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

suspend inline fun <T, R> T.myLetSuspend(block: suspend (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

@IgnorableReturnValue
suspend fun ignorableSuspend(): String = "ignorable"

suspend fun nonIgnorableSuspend(): String = "non-ignorable"

suspend fun testSuspendLambdaIgnorable(s: String) {
    s.myLetSuspend { ignorableSuspend() }
}

suspend fun testSuspendLambdaNonIgnorable(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLetSuspend<!> { nonIgnorableSuspend() }
}

suspend fun testSuspendLambdaWithSuspendCall(s: String, sb: StringBuilder) {
    s.myLetSuspend { sb.append(it) }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, lambdaLiteral, nullableType, suspend, thisExpression, typeParameter */
