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

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testNestedLabelledReturns(s: String, flag: Boolean) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> outer@{
        if (flag) return@outer fooS()
        it.myLet inner@{
            return@inner fooS()
        }
    }

    s.myLet outer@{
        if (flag) return@outer ign()
        it.myLet inner@{
            return@inner ign()
        }
    }
}

fun testMixedReturnsInLabelled(s: String, flag: Boolean) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        if (flag) return@myLet ign()
        fooS()
    }

    s.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        if (flag) return@myLet fooS()
        ign()
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, inline, lambdaLiteral, nullableType, thisExpression, typeParameter */
