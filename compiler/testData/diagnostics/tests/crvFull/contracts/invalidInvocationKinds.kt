// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun <T, R> T.myLetAtLeastOnce(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

fun <T, R> T.myLetAtMostOnce(block: (T) -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

fun <T, R> T.myLetUnknown(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
        returnsResultOf(block)
    }
    return block(this)
}

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testAtLeastOnce(s: String, sb: StringBuilder) {
    s.myLetAtLeastOnce { sb.append(it) }
    s.<!RETURN_VALUE_NOT_USED!>myLetAtLeastOnce<!> { fooS() }
    s.myLetAtLeastOnce { ign() }
}

fun testAtMostOnce(s: String, sb: StringBuilder) {
    s.myLetAtMostOnce { sb.append(it) }
    s.<!RETURN_VALUE_NOT_USED!>myLetAtMostOnce<!> { fooS() }
    s.myLetAtMostOnce { ign() }
}

fun testUnknown(s: String, sb: StringBuilder) {
    s.myLetUnknown { sb.append(it) }
    s.<!RETURN_VALUE_NOT_USED!>myLetUnknown<!> { fooS() }
    s.myLetUnknown { ign() }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, flexibleType, funWithExtensionReceiver, functionDeclaration,
functionalType, javaFunction, lambdaLiteral, nullableType, stringLiteral, thisExpression, typeParameter */
