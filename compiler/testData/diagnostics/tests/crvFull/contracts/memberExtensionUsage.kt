// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

class Outer {
    inline fun <T, R> T.myLet(block: (T) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            returnsResultOf(block)
        }
        return block(this)
    }

    fun testMemberExtensionVisibility(s: String?) {
        s?.<!RETURN_VALUE_NOT_USED!>myLet<!> { fooS() }
        s?.myLet { ign() }
    }

    inner class Inner {
        fun testInnerClassAccess(s: String?) {
            s?.<!RETURN_VALUE_NOT_USED!>myLet<!> { fooS() }
            s?.myLet { ign() }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration,
functionalType, inline, lambdaLiteral, nullableType, safeCall, thisExpression, typeParameter */
