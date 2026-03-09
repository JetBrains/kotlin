// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +ImprovedAliasTracking

@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.*

var x: Int = 3

fun foo(n: Int) {
    contract { local(n) }
    <!LEAKED_LOCAL("n: Int")!>x = n<!>
}

fun foo0(n: Int) {
    contract { local(n) }
    x = n + 1
}

fun bar(n: Int) {
    contract { local(n) }
    val y = n
    <!LEAKED_LOCAL("n: Int")!>x = y<!>
}

fun bar1(n: Int) {
    contract { local(n) }
    val y = n
    x = y + 1
}

fun bar2(n: Int) {
    contract { local(n) }
    val y = n + 1
    x = y
}

fun quux(n: Int): Int {
    contract { local(n) }
    val y = n
    <!LEAKED_LOCAL("n: Int")!>return y<!>
}

fun quux1(n: Int): Int {
    contract { local(n) }
    val y = n + 1
    return y
}

fun quux2(n: Int): Int {
    contract { local(n) }
    val y = n
    return y + 1
}

fun moo(n: Int): Int {
    contract { local(n) }
    return when {
        n > 0 -> <!LEAKED_LOCAL("n: Int")!>n<!>
        else -> 1
    }
}

/* GENERATED_FIR_TAGS: assignment, classReference, contracts, functionDeclaration, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration */
