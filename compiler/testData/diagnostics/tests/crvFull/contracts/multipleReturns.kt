// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T, R> T.myRun(block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsResultOf(block)<!>
    }
    return block()
}

val getBool: Boolean = true

fun testStuff(a: Int, s: String?, list: MutableList<String>): Boolean {
    s?.myRun {
        if (a > 5) return list.add(this) // non-local, should not be counted
        if (a > 4) return@myRun getBool // non-ignorable
        if (a > 3) return@myRun list.add(this) // ignorable
        /*2*/getBool // not last statement, should not be counted
        /*1*/list.add(this) // ignorable
    } // 4 * 3 * 1 = non-ignorable

    s?.myRun {
        if (a > 5) return list.add(this) // non-local, should not be counted
        if (a > 4) return@myRun list.add(this) // ignorable
        if (a > 3) return@myRun list.add(this) // ignorable
        /*2*/list.add(this) // not last statement, should not be counted
        /*1*/getBool
    } // 4 * 3 * 1 = non-ignorable

    s?.myRun {
        if (a > 5) return getBool // non-local, should not be counted
        if (a > 4) return@myRun list.add(this) // ignorable
        if (a > 3) return@myRun getBool // non-ignorable
        /*2*/getBool // not last statement, should not be counted
        /*1*/list.add(this)
    } // 4 * 3 * 1 = non-ignorable

    s?.myRun {
        if (a > 5) return getBool // non-local, should not be counted
        if (a > 4) return@myRun list.add(this) // ignorable
        if (a > 3) return@myRun list.add(this) // ignorable
        /*2*/getBool // not last statement, should not be counted
        /*1*/list.add(this)
    } // 4 * 3 * 1 = ignorable

    return false
}

/* GENERATED_FIR_TAGS: comparisonExpression, contractCallsEffect, contracts, funWithExtensionReceiver,
functionDeclaration, functionalType, ifExpression, inline, integerLiteral, lambdaLiteral, nullableType,
propertyDeclaration, safeCall, thisExpression, typeParameter, typeWithExtension */
