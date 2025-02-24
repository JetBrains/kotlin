// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +AllowContractsOnSomeOperators

import kotlin.contracts.*

class A(var v: Int = 0)

// unary plus

operator fun Any.unaryPlus(): Boolean {
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        returns(true) implies (this@unaryPlus is Int)
    }
    return this is Int
}

fun test_plus(x: Any) {
    if (+x) {
        x.<!UNRESOLVED_REFERENCE!>toChar<!>()
    }
}

// unaryMinus

operator fun Any.unaryMinus(): Boolean {
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        returns(true) implies (this@unaryMinus is String)
    }
    return this is String
}

fun test_unaryMinus(x: Any) {
    if (-x) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

// not

operator fun Any.not(): Boolean {
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        returns(false) implies (this@not is Int)
    }
    return this !is Int
}

fun test_not(x: Any) {
    if (!!x) {
        x.<!UNRESOLVED_REFERENCE!>toChar<!>()
    }
}

// inc and dec

operator fun Any.inc(): Int {
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        returns() implies (this@inc is Int)
    }
    return (this as Int) + 1
}

operator fun Any.dec(): Int {
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        returns() implies (this@dec is Int)
    }
    return (this as Int) - 1
}

fun test_inc_dec(ix1: Any, ix2: Any) {
    var x1 = ix1
    x1++
    x1.<!UNRESOLVED_REFERENCE!>toChar<!>()
    var x2 = ix2
    x2--
    x2.<!UNRESOLVED_REFERENCE!>toChar<!>()
}

// invoke

operator fun A.invoke(i: Int?): Int {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (i != null) }
    return v
}

fun test_invoke(a: A, i: Int?) {
    if (a(i) == 0) {
        i <!UNSAFE_OPERATOR_CALL!>+<!> 1
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
    <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    return Range(this, body())
}

operator fun A.rangeUntil(body: () -> A): Range {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    return Range(this, body())
}

operator fun Range.contains(element: A?): Boolean {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(true) implies (element != null) } // KT-34132
    if (element == null) return false
    return (from.v..(to?.v ?: 100)).contains(<!DEBUG_INFO_SMARTCAST!>element<!>.v)
}

operator fun Any.iterator(): It {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (this@iterator is Range) }
    this@iterator as Range
    return It(<!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>from<!>, <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>to<!>)
}

fun test_ranges(r: Any, aa: A?) {
    var a = A()
    val <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>r1b<!>: Boolean;
    val r1 = a..{ <!CAPTURED_VAL_INITIALIZATION!>r1b<!> = true; A(1) }
    val <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>r2b<!>: Boolean;
    val r2 = a..<{ <!CAPTURED_VAL_INITIALIZATION!>r2b<!> = true; A(1) }

    val x =
        if (aa in r1) aa<!UNSAFE_CALL!>.<!>v //KT-34132
        else 0

    for (y in r) {
        r.<!UNRESOLVED_REFERENCE!>from<!>
    }
}
