// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun <T, R> T.myLetTryFinally(block: (T) -> R): R {
    contract {
        returnsResultOf(block)
    }
    try {
        return block(this)
    } finally {
    }
}

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testTryFinallyMeaningful(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLetTryFinally<!> { fooS() }
}

fun testTryFinallyIgnorable(s: String, sb: StringBuilder) {
    s.myLetTryFinally { sb.append(it) }
}

fun testTryFinallyTopLevelIgnorable(s: String) {
    s.myLetTryFinally { ign() }
}

/* GENERATED_FIR_TAGS: contracts, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
javaFunction, lambdaLiteral, nullableType, stringLiteral, thisExpression, tryExpression, typeParameter */
