// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun <T, R> T.myLetNoCallsInPlace(block: (T) -> R): R {
    contract {
        returnsResultOf(block)
    }
    return block(this)
}

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testWithoutCallsInPlace(s: String, sb: StringBuilder) {
    s.myLetNoCallsInPlace { sb.append(it) }
    s.<!RETURN_VALUE_NOT_USED!>myLetNoCallsInPlace<!> { fooS() }
    s.myLetNoCallsInPlace { ign() }
}

/* GENERATED_FIR_TAGS: contracts, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
javaFunction, lambdaLiteral, nullableType, stringLiteral, thisExpression, typeParameter */
