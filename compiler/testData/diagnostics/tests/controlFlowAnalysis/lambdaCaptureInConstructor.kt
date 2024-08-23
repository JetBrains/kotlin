// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUES: KT-70133, KT-70724

import kotlin.contracts.*

fun test1() {
    val x: String
    Array(
        init = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        size = if (true) { x = ""; 1 } else { x = ""; 1 }
    )
}

class A {
    constructor(x: () -> Int, y: Int){
        contract { callsInPlace(x, InvocationKind.EXACTLY_ONCE) }
        x()
    }
}

fun test2() {
    val x: String
    A(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        if (true) { x = ""; 1 } else { x = ""; 1 }
    )
}

class B(val x: () -> Int, val y: Int)

fun test3() {
    val x: String
    B(
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        if (true) { x = ""; 1 } else { x = ""; 1 }
    )
}