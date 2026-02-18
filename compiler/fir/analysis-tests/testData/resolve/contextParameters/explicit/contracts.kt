// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ExperimentalContracts
context(a: Int?)
fun foo0() {
    contract {
        returns() implies (a != null)
    }
}

@ExperimentalContracts
context(a: Any)
fun foo1() {
    contract {
        returns() implies (a is Boolean)
    }
}

@ExperimentalContracts
context(a: Any, b: Number)
fun foo2(): Boolean {
    contract {
        returns(false) implies (a is Boolean && b is Int)
    }
    return false
}

@OptIn(ExperimentalContracts::class)
fun test0(int: Int?) {
    with(int) {
        foo0()
        val tmp: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> int
        val tmp1: Int = this
    }
    foo0(a = int)
    val tmp: Int = int
}

@OptIn(ExperimentalContracts::class)
fun test1(any: Any) {
    with(any) {
        foo1()
        if (<!CONDITION_TYPE_MISMATCH!>any<!>) {}
        if (this) {}
    }
    foo1(a = any)
    if (any) {}
}

@OptIn(ExperimentalContracts::class)
fun test2(any: Any, number: Number) {
    if (!foo2(a = any, b = number)) {
        if (any) {
            val tmp: Int = number
        }
    }
}

/* GENERATED_FIR_TAGS: andExpression, contractConditionalEffect, contracts, equalityExpression, functionDeclaration,
functionDeclarationWithContext, ifExpression, isExpression, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, smartcast */
