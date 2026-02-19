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

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun unit(): Unit = Unit

fun testLiterals(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> { 42 }
    s.myLet { it }
}

fun testFunctions(s: String?) {
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> { fooS() }
    s?.myLet { ign() }
    s?.myLet { unit() }
}

fun testOperators(s: String, m: MutableList<String>) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> { "" + it }
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> { "foo$it" }
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> { String::class }

    // This is not reported because operator MutableList.set is ignorable, but will be reported for non-unit .set in general
    s.myLet { m[0] = it }
}

fun testIgnorability(s: String?, sb: StringBuilder) {
    s?.myLet { sb.append(it) }
}

fun testMultiline(s: String?, sb: StringBuilder) {
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        val x = 1 + 2
        x + 3
    }

    // These calls are reported now because myLet() declaration has must-use status of its own

    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        it.last().<!RETURN_VALUE_NOT_USED!>myLet<!> {
        it + "b"
    }
        it.last().myLet {
            it + "b"
        }
    }

    s?.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        it.last().myLet {
            sb.append(it)
        }
        it.last().myLet {
            sb.append(it)
        }
    }
}

fun testNonLocalReturn(s: String?): Int {
    s?.myLet<String, Nothing> { return 42 }
    s?.myLet<String, String> { return 42 }
    return 0
}

fun testNoExplicitReturns(s: String?) {
    s?.myLet<String, Nothing> { throw IllegalStateException("a") }
    s?.myLet<String, String> { throw IllegalStateException("a") }
}

/* GENERATED_FIR_TAGS: assignment, contractCallsEffect, contractReturnsResultOfEffect, contracts, functionDeclaration,
functionalType, inline, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */
