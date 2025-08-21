// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +AllowContractsOnSomeOperators

import kotlin.contracts.*

class A(var v: Int = 0)

// unary plus

operator fun Any.unaryPlus(): Boolean {
    contract {
        returns(true) implies (this@unaryPlus is Int)
    }
    return this is Int
}

fun test_plus(x: Any) {
    if (+x) {
        x.toChar()
    }
}

// unaryMinus

operator fun Any.unaryMinus(): Boolean {
    contract {
        returns(true) implies (this@unaryMinus is String)
    }
    return this is String
}

fun test_unaryMinus(x: Any) {
    if (-x) {
        x.length
    }
}

// not

operator fun Any.not(): Boolean {
    contract {
        returns(false) implies (this@not is Int)
    }
    return this !is Int
}

fun test_not(x: Any) {
    if (!!x) {
        x.toChar()
    }
}

// inc and dec

operator fun Any.inc(): Int {
    contract {
        returns() implies (this@inc is Int)
    }
    return (this as Int) + 1
}

operator fun Any.dec(): Int {
    contract {
        returns() implies (this@dec is Int)
    }
    return (this as Int) - 1
}

fun test_inc_dec(ix1: Any, ix2: Any) {
    var x1 = ix1
    x1++
    x1.toChar()
    var x2 = ix2
    x2--
    x2.toChar()
}

// invoke

operator fun A.invoke(i: Int?): Int {
    contract { returns() implies (i != null) }
    return v
}

fun test_invoke(a: A, i: Int?) {
    if (a(i) == 0) {
        i + 1
    }
}

// range and related operators

class Range(val from : A, val to: A?)

class It(val from: A, val to: A?) {
    var a = from.v

    operator fun next(): A {
        val next = A(a)
        a++
        return next
    }

    operator fun hasNext(): Boolean = a <= to?.v ?: 100
}

operator fun A.rangeTo(body: () -> A): Range {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    return Range(this, body())
}

operator fun A.rangeUntil(body: () -> A): Range {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    return Range(this, body())
}

operator fun Range.contains(element: A?): Boolean {
    contract { returns(true) implies (element != null) } // KT-34132
    if (element == null) return false
    return (from.v..(to?.v ?: 100)).contains(element.v)
}

operator fun Any.iterator(): It {
    contract { returns() implies (this@iterator is Range) }
    this@iterator as Range
    return It(from, to)
}

fun test_ranges(r: Any, aa: A?) {
    var a = A()
    val r1b: Boolean;
    val r1 = a..{ r1b = true; A(1) }
    val r2b: Boolean;
    val r2 = a..<{ r2b = true; A(1) }

    val x =
        if (aa in r1) aa.v //KT-34132
        else 0

    for (y in r) {
        r.from
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, asExpression, assignment, classDeclaration, comparisonExpression,
contractCallsEffect, contractConditionalEffect, contracts, elvisExpression, equalityExpression, forLoop,
funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression, incrementDecrementExpression,
integerLiteral, isExpression, lambdaLiteral, localProperty, nullableType, operator, primaryConstructor,
propertyDeclaration, rangeExpression, safeCall, smartcast, thisExpression, unaryExpression */
