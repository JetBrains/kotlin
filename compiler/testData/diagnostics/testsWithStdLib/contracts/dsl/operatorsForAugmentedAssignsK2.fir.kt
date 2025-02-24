// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// Currently forbidden, see KT-77175
// LANGUAGE: +AllowContractsOnSomeOperators

import kotlin.contracts.*

class A(var v: Int = 0)

// plus

operator fun Boolean.plus(x: Boolean): Boolean {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (x) }
    return x
}

fun test_plus(x: Any) {
    if (true + (x is String)) {
        x.length
    }
}

// minus

operator fun Boolean.minus(x: Boolean): Boolean {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (x) }
    return x
}

fun test_minus(x: Any) {
    if (true - (x is Int)) {
        x.toChar()
    }
}

// *Assign

operator fun A.plusAssign(body: () -> Int) {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    v += body()
}

operator fun A.minusAssign(body: () -> Int) {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    v -= body()
}

operator fun A.timesAssign(body: () -> Int) {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    v *= body()
}

operator fun A.divAssign(body: () -> Int) {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    v /= body()
}

operator fun A.remAssign(body: () -> Int) {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    v %= body()
}

fun test_xAssign() {
    var a = A()
    val plus: Boolean; a += { plus = true; 1 }
    val minus: Boolean; a -= { minus = true; 1 }
    val times: Boolean; a *= { times = true; 1 }
    val div: Boolean; a /= { div = true; 1 }
    val rem: Boolean; a %= { rem = true; 1 }
}

// indexed access

operator fun A.get(i: Int?): Int {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (i != null) }
    return v
}

operator fun A?.set(i: Int, vnew: Int) {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (this@set != null) }
    this!!
    v = vnew
}

fun test_indexed(a1: A, i: Int?, a2: A?) {
    if (a1[i] == 0) {
        i + 1
    }
    a2[0] = 1
    a2.v
}
