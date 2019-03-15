// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun myRun(block: () -> Unit): Unit {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun getBool(): Boolean = false

fun withLabeledReturn() {
    val y: Int

    val x = myRun outer@ {
        myRun { return@outer Unit }
        <!UNREACHABLE_CODE!>y = 42<!>
    }

    println(<!UNINITIALIZED_VARIABLE!>y<!>)
    println(x)
}

fun withLabeledReturn2(y: Int) {
    myRun outer@ {
        myRun { return@outer Unit }
        <!UNREACHABLE_CODE!>println(y)<!>
    }
    println(y)
}