// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T, R> T.selector(condition: (T) -> Boolean, a: (T) -> R, b: (T) -> R): R {
    contract {
        callsInPlace(a, InvocationKind.AT_MOST_ONCE)
        callsInPlace(b, InvocationKind.AT_MOST_ONCE)
        returnsResultOf(a)
        returnsResultOf(b)
    }
    return if (condition(this)) a(this) else b(this)
}

fun testSelector(s: String, sb: StringBuilder): Int {
    val cond = String::isEmpty
    s.<!RETURN_VALUE_NOT_USED!>selector<!>(cond, { it + "a" }, { it + "b" }) // both non-ignorable
    s.<!RETURN_VALUE_NOT_USED!>selector<!>(cond, { sb.append(it) }, { it + "b" }) // one non-ignorable
    s.<!RETURN_VALUE_NOT_USED!>selector<!>(cond, { it + "a" }, sb::append) // one non-ignorable
    s.selector(cond, { sb.append(it) }, sb::append) // both ignorable => ignorable
    s.selector(cond, { sb.append(it) }, { return 42 }) // ignorable, non-local return => ignorable
    s.<!RETURN_VALUE_NOT_USED!>selector<!>(cond, { return 42 }, { "return 42" }) // non-local return, non-ignorable => non-ignorable

    return 0
}

// Test that lambdas which are not mentioned in contract do not propagate ignorability:
fun testIgnorableInCondition(s: String, sb: StringBuilder, list: MutableList<String>) {
    s.<!RETURN_VALUE_NOT_USED!>selector<!>(
        { list.add(it) },
        sb::append,
        { it + "b" }
    )

    s.selector(
        { list.add(it) },
        sb::append,
        sb::append,
    )
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, inline, integerLiteral, lambdaLiteral, nullableType, safeCall, thisExpression, typeParameter */
