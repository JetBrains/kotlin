// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun <T, R> T.myLetNonInline(block: (T) -> R): R {
    contract {
        returnsResultOf(block)
    }
    return block(this)
}

@IgnorableReturnValue
fun ignorableOp(s: String): String = s

fun nonIgnorableOp(s: String): String = s + "!"

fun testNonInlineDirectReferences(s: String) {
    s.myLetNonInline(::ignorableOp)
    s.<!RETURN_VALUE_NOT_USED!>myLetNonInline<!>(::nonIgnorableOp)
}

fun testNonInlineLambdas(s: String) {
    s.myLetNonInline { ignorableOp(it) }
    s.<!RETURN_VALUE_NOT_USED!>myLetNonInline<!> { nonIgnorableOp(it) }
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, contracts, funWithExtensionReceiver, functionDeclaration,
functionalType, lambdaLiteral, nullableType, stringLiteral, thisExpression, typeParameter */
