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

fun testConditionals(s: String?) {
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> { if (it.length > 0) a() else b() }
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> { 42 }
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        when (it.length) {
            0 -> a()
            1 -> b()
            else -> ign()
        }
    }
    s?.myLet {
        when (it.length) {
            0 -> ign()
            else -> ign()
        }
    }
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        if (it.length > 0) return@myLet a()
        if (it.length == 0) ign() else ign()
    }
}

fun testSpecialCases(s: String?, list: MutableList<String>) {
    s?.myLet { list.add(it) }
    s?.myLet { list[0] = it }
    s?.myLet { it.isEmpty() || error("") }
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> { it.isEmpty() || it.length > 0 }
}

fun testRecursive(a: String?, b: String?) {
    a?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        b?.myLet {
            a()
        }
    }

    a?.myLet {
        b?.myLet {
            ign()
        }
    }

    a?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        if (it.length > 0) b?.myLet {
            a()
        } else ign()
    }

    a?.myLet {
        if (it.length > 0) ign() else b?.myLet {
            ign()
        }
    }
}

fun testMultiple(a: String?, b: String?) {
    a?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        if (it.length > 0) b?.myLet { a() } else b?.myLet { b() }
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, lambdaLiteral, nullableType, stringLiteral, thisExpression, typeParameter */
