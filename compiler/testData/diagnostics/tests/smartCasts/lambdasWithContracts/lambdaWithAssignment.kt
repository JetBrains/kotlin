// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUES: KT-83419

import kotlin.contracts.*

fun myRequire(condition: Boolean, block: () -> Unit) {
    contract {
        returns() implies condition
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    require(condition)
}

fun myHoldsIn(condition: Boolean, block: () -> Unit) {
    contract {
        condition holdsIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (condition) block()
}

fun test1() {
    var x: Any? = 41
    myRequire(x is Int) {
        x <!UNRESOLVED_REFERENCE!>+<!> 1
        x = (null as Any?)
        x <!UNRESOLVED_REFERENCE!>+<!> 1
    }
    x + 1 // bad: ClassCastException
}

fun test2() {
    var x: Any? = 41
    myRequire(x is Int) {
        x <!UNRESOLVED_REFERENCE!>+<!> 1
    }
    x + 1
}

fun test3() {
    var x: Any? = 41
    myHoldsIn(x is Int) {
        x + 1
        x = (null as Any?)
        x <!UNRESOLVED_REFERENCE!>+<!> 1
    }
    x <!UNRESOLVED_REFERENCE!>+<!> 1
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, contractConditionalEffect, contracts, functionDeclaration,
functionalType, inline, integerLiteral, isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
smartcast, stringLiteral */
