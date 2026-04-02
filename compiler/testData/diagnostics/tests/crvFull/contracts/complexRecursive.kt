// RUN_PIPELINE_TILL: BACKEND
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

fun a() = true
fun b() = true

@IgnorableReturnValue fun ign() = false

fun test1(a: String?, b: String?): Int {
    a?.myLet {
        b?.myLet { return a.length } ?: ign()
    }
    a?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        b?.myLet { return a.length } ?: a()
    }
    a?.myLet {
        b?.myLet { return if (it.length > 4) 0 else 1 }
    }
    return 0
}

fun test2(a: String?, b: String?): Int {
    a?.<!RETURN_VALUE_NOT_USED!>myLet<!> outer@{
        b?.myLet {
            if (it.length > 5) return@outer 1 else ign()
        }
    }
    a?.<!RETURN_VALUE_NOT_USED!>myLet<!> outer@{
        b?.myLet {
            if (it.length > 5) return@outer ign() else 1
        }
    }
    a?.myLet outer@{
        b?.myLet {
            if (it.length > 5) return@outer ign() else ign()
        }
    }
    return 0
}

/* GENERATED_FIR_TAGS: comparisonExpression, contractCallsEffect, contracts, elvisExpression, funWithExtensionReceiver,
functionDeclaration, functionalType, ifExpression, inline, integerLiteral, intersectionType, lambdaLiteral, nullableType,
safeCall, smartcast, thisExpression, typeParameter */
